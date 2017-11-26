package com.flowy.bittrexExchange

import java.time.{Instant, ZoneOffset}
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import com.flowy.trailingStop.messages.TrailingStop
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models.MarketStructures.MarketUpdate
import com.flowy.common.models.{BasicUserData, Trade, TradeRequest, TradeStatus}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext
import redis.RedisClient

import scala.tools.reflect.ToolBox


object MarketTradeService {
  def props(marketName: String, bagel: TheEverythingBagelDao, redis: RedisClient)(implicit context: ExecutionContext) =
    Props(new MarketTradeService(marketName, bagel, redis))

  case class PostTrade(forUser: BasicUserData, request: TradeRequest, sender: Option[ActorRef] = None)
  case class UpdateTrade(forUser: BasicUserData, request: TradeRequest, sender: Option[ActorRef] = None)
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

  val buyConditions = collection.mutable.ListBuffer[TradeBuyCondition]()

  val dynamic = currentMirror.mkToolBox()

  val mediator = DistributedPubSub(context.system).mediator


  override def preStart() = {
    // load pending conditions from bagel
    bagel.findTradesByStatus(marketName, TradeStatus.Pending).map { pendingTrades =>
      if (pendingTrades.length > 0) {
        // map all the conditions into a single collection
        val pendingConditions = pendingTrades.map( t => TradeBuyCondition(t.id, t.buyConditions) )

        log.info(s"$marketName loading pending trades")
        buyConditions.append(pendingConditions: _*)
      }
    }

    log.info(s"$marketName actor started")
  }

  def receive: Receive = {
    case update: MarketUpdate =>
      updateState(update)

    case PostTrade(user, request, Some(sender)) =>
      postTrade(user, request, sender)

    case UpdateTrade(user, request, Some(sender)) =>
      updateTrade(user, request, sender)
  }


  private def executeTrades(lastPrice: Double, conditions: List[TradeBuyCondition]) = {
    val sellConditions = scala.collection.mutable.Seq[TradeBuyCondition]()

    conditions.foreach { condish =>
      val tradeId = condish.tradeId

      bagel.findTradeById(tradeId).map { t =>

        if (t.isDefined) {
          val trade = t.get
          // TODO this is where the order shall be executed via the BittrexClient

          val updatedTrade = t.get.copy(
            // TODO the buyPrice should be the actual price you may need to read this from bittrex
            buyPrice = Some(lastPrice),
            buyTime = Some(Instant.now().atOffset(ZoneOffset.UTC)),
            status = TradeStatus.Bought
          )

          log.info(s"buy ${trade.quantity} ${trade.marketName} for user: ${trade.userId}")
          bagel.updateTrade(updatedTrade)

          // TODO this needs refinement
          if (trade.sellConditions.isDefined) {
            val sellConditions = trade.sellConditions.get
            val conditions = sellConditions.split(" or ")

            conditions.foreach{ c =>
              if (c.contains("TrailingStop")) {
                val extractParams = """^.*?TrailingStop\((0\.\d{2}),\s(\d+\.\d+).*?""".r
                c match {
                  case extractParams(percent, refPrice) =>
                    val trailStop = TrailingStop(trade.userId, tradeId, trade.marketName, percent.toDouble, refPrice.toDouble)
                    mediator ! Publish("TrailingStop", TrailingStop(trade.userId, tradeId, trade.marketName, percent.toDouble, refPrice.toDouble))
                    log.info(s"sending trailing stop $trailStop")
                  case _ =>
                  // do nothing
                }
              }
            }
          }
        }
      }
    }
  }

  /**
    * Updates the internal state of this service.
    * @param update
    * @return
    */
  private def updateState(update: MarketUpdate) = {
    val lastPrice = update.Last

    // get all buy conditions that pass
    val conditionsThatPass = buyConditions.filter { cond =>
      val condition = cond.conditionEx.replace("price", lastPrice.toString)
      dynamic.eval(dynamic.parse(s"$condition")) == true
    }

    executeTrades(lastPrice, conditionsThatPass.toList)

    // remove the conditions that have passed
    buyConditions --= conditionsThatPass
  }

  private def postTrade(user: BasicUserData, request: TradeRequest, sender: ActorRef) = {
    val trade = Trade.fromRequest(request, user.id)

    bagel.insert(trade).map { result =>
      if (result > 0) {
        val conditions = trade.buyConditions

        buyConditions.append(TradeBuyCondition(trade.id, conditions))

        sender ! true
      } else {
        sender ! false
      }
    }
  }

  private def updateTrade(user: BasicUserData, request: TradeRequest, sender: ActorRef) = {
    val trade = Trade.fromRequest(request, user.id)
  }
}


