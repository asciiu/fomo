package com.flowy.bexchange

import java.time.{Instant, ZoneOffset}
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import com.flowy.trailstop.TrailingStop
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models.MarketStructures.MarketUpdate
import com.flowy.common.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import redis.RedisClient

import scala.tools.reflect.ToolBox
import scala.util.{Failure, Success}


object MarketTradeService {
  def props(marketName: String, bagel: TheEverythingBagelDao, redis: RedisClient)(implicit context: ExecutionContext) =
    Props(new MarketTradeService(marketName, bagel, redis))

  case class DeleteTrade(trade: Trade, senderOpt: Option[ActorRef] = None)
  case class PostTrade(forUser: UserData, request: TradeRequest, senderOpt: Option[ActorRef] = None)
  case class UpdateTrade(forUser: UserData, tradeId: UUID, request: TradeRequest, senderOpt: Option[ActorRef] = None)
}


/**
  * Services a single market: example BTC-NEO
  * @param marketName
  * @param bagel
  * @param redis
  */
class MarketTradeService(val marketName: String, bagel: TheEverythingBagelDao, redis: RedisClient) extends Actor
  with ActorLogging {

  import MarketTradeService._
  import scala.reflect.runtime.currentMirror

  implicit val akkaSystem = context.system

  case class TradeBuyCondition(tradeId: UUID, conditionEx: String)

  val trades = collection.mutable.ListBuffer[Trade]()

  val dynamic = currentMirror.mkToolBox()

  val mediator = DistributedPubSub(context.system).mediator


  override def preStart() = {
    // load pending conditions from bagel
    bagel.findTradesByStatus(marketName, TradeStatus.Pending).map { pendingTrades =>
      trades ++= pendingTrades
    }

    log.info(s"$marketName actor started")
  }

  def receive: Receive = {
    case update: MarketUpdate =>
      updateState(update)

    case PostTrade(user, request, Some(sender)) =>
      postTrade(user, request, sender)

    case UpdateTrade(user, tradeId, request, Some(sender)) =>
      updateTrade(user, tradeId, request, sender)

    case DeleteTrade(trade, Some(sender)) =>
      deleteTrade(trade, sender)

    case x =>
      log.warning(s"received unknown message - $x")
  }

  private def executeBuyTrades(lastPrice: Double) = {
    val buyTrades = trades.filter { trade =>
      val condition = trade.buyConditions.replace("price", lastPrice.toString)
      dynamic.eval(dynamic.parse(s"$condition")) == true
    }.filter { trade =>
      trade.status == TradeStatus.Pending
    }

    buyTrades.foreach { trade =>
      // TODO this is where the order shall be executed via the BittrexClient

      // TODO the buyPrice should be the actual price you may need to read this from bittrex
      log.info(s"buy ${trade.baseQuantity} ${trade.info.marketName} for user: ${trade.userId}")
      val updatedTrade = trade.copy(
        stat = TradeStat(
          buyPrice = Some(lastPrice),
          buyTime = Some(Instant.now().atOffset(ZoneOffset.UTC))),
        status = TradeStatus.Bought
      )

      // TODO don't assume this update succeeded
      bagel.updateTrade(updatedTrade)
      trades -= trade

      trade.stopLossConditions.map { sellConditions =>
        sellConditions.split(" or ").foreach { c =>
          if (c.contains("TrailingStop")) {
            // TODO regex pattern for simple price conditions
            val extractParams =
              """^.*?TrailingStop\((0\.\d{2}),\s(\d+\.\d+).*?""".r
            c match {
              case extractParams(percent, refPrice) =>
                val trailStop = TrailingStop(trade.userId, trade.id, trade.info.marketName, percent.toDouble, refPrice.toDouble)
                mediator ! Publish("TrailingStop", TrailingStop(trade.userId, trade.id, trade.info.marketName, percent.toDouble, refPrice.toDouble))
                log.info(s"sending trailing stop $trailStop")
              case _ =>
                log.warning(s"Leo is sent in an invalid format for the TrailingStop got: $c should be TrailingStop(percent, startTopPriceAt)")
            }
          } else {
            // assume simple conditions here
            trades.append(updatedTrade)
          }
        }

        // no need to return anything from this map
        Unit
      }
    }
  }

  private def executeSellTrades(lastPrice: Double) = {
    val sellTrades = trades.filter { trade =>
      val stopLoss = trade.stopLossConditions.getOrElse("").replace("price", lastPrice.toString)
      val stopProfit = trade.takeProfitConditions.getOrElse("").replace("price", lastPrice.toString)

      (stopLoss != "" && dynamic.eval(dynamic.parse(s"$stopLoss")) == true) ||
      (stopProfit != "" && dynamic.eval(dynamic.parse(s"$stopProfit")) == true)
    }.filter { trade =>
      trade.status == TradeStatus.Bought
    }

    sellTrades.foreach { trade =>
      // TODO this is where the order shall be executed via the BittrexClient

      // TODO the sellPrice should be the actual price you may need to read this from bittrex
      log.info(s"sell ${trade.info.marketName} for user: ${trade.userId}")
      val updatedTrade = trade.copy(
        stat = TradeStat(
          sellPrice = Some(lastPrice),
          sellTime = Some(Instant.now().atOffset(ZoneOffset.UTC))),
        status = TradeStatus.Sold
      )

      // TODO don't assume this update succeeded
      bagel.updateTrade(updatedTrade)
      trades -= trade

      // TODO may need to communicate this to cancel trailing stop
      //mediator ! Publish("CancelTrailingStop", TrailingStop(trade.userId, trade.id, trade.info.marketName))
    }
  }

  /**
    * Updates the internal state of this service.
    * @param update
    * @return
    */
  private def updateState(update: MarketUpdate) = {
    Future(executeBuyTrades(update.Last))
    Future(executeSellTrades(update.Last))
  }

  /**
    * Add trade to DB and insert buy conditions.
    * @param user
    * @param request
    * @param senderRef response to sender when finished with Some(trade) or None
    * @return
    */
  private def postTrade(user: UserData, request: TradeRequest, senderRef: ActorRef) = {
    val trade = Trade.fromRequest(request, user.id)

    log.info(s"MarketTradeService.postTrade - $request")

    val stuff = for {
      result <- bagel.insert(trade)
      balance <- bagel.findBalance(user.id, trade.apiKeyId, trade.info.baseCurrency)
    } yield (result, balance)

    stuff.onComplete {
      // trade insert result willl be > 0 for success
      // and balance must be > base quantity
      case Success((result, Some(balance))) if (result > 0 && balance.availableBalance > trade.baseQuantity) =>
        val newBalance = balance.copy(availableBalance = balance.availableBalance - trade.baseQuantity)
        bagel.updateBalance(newBalance)

        trades.append(trade)

        // send trade response to sender
        senderRef ! Some(trade)
      case _ =>
        senderRef ! None
    }
  }

  private def deleteTrade(trade: Trade, sender: ActorRef) = {
    // cannot cancel a sold trade because it is already finished.
    trade.status match {
      case TradeStatus.Sold =>
        // cannot cancel a sold trade because it is already finished.
        sender ! None

      case TradeStatus.Pending =>
        // delete the trade from the system if pending
        for {
          deletedOpt <- bagel.deleteTrade(trade)
          balanceOpt <- bagel.findBalance(trade.userId, trade.apiKeyId, trade.info.baseCurrency)
        } yield {

          (deletedOpt, balanceOpt) match {
            case (Some(deleted), Some(balance)) =>
              bagel.updateBalance(balance.copy(availableBalance = balance.availableBalance + deleted.baseQuantity)).map ( _ => sender ! Some(deleted))

            case _ =>
              sender ! None
          }
        }

      case _ =>
        // all other trades statuses are cancellable
        bagel.updateTrade(trade.copy(status = TradeStatus.Cancelled)).map (sender ! _)
    }
  }

  private def updateTrade(user: UserData, tradeId: UUID, request: TradeRequest, sender: ActorRef) = {
    bagel.findTradeById(tradeId).map {
      // not permitted to change someone elses trade
      case Some(trade) if trade.userId != user.id =>
        sender ! None

      // trade status pending update quantity and conditions
      case Some(trade) if trade.status == TradeStatus.Pending =>
        bagel.updateTrade(
          trade.copy(
            baseQuantity = request.baseQuantity,
            buyConditions = request.buyConditions,
            stopLossConditions = request.stopLossConditions,
            takeProfitConditions = request.takeProfitConditions)
        ).map { updated =>
          sender ! updated
        }

      // trade status bought update sellconditions only
      case Some(trade) if trade.status == TradeStatus.Bought =>
        bagel.updateTrade(
          trade.copy(
            stopLossConditions = request.stopLossConditions,
            takeProfitConditions = request.takeProfitConditions)
        ).map { updated =>
          sender ! updated
        }

      case None =>
        sender ! None
    }
  }
}


