package com.flowy.bexchange.trade

import java.time.{Instant, ZoneOffset}

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import com.flowy.bexchange.MarketTradeService
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models.MarketStructures.MarketUpdate
import com.flowy.common.models._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.tools.reflect.ToolBox


object TradeActor {
  def props(trade: Trade, bagel: TheEverythingBagelDao)(implicit context: ExecutionContext) =
    Props(new TradeActor(trade, bagel))

  case class Buy(price: Double)
  case class Sell(price: Double)
  case class DeleteTrade(senderOpt: Option[ActorRef] = None)
  case class UpdateTrade(request: TradeRequest, senderOpt: Option[ActorRef] = None)
}


/**
  * Services a single market: example BTC-NEO
  * @param trade
  * @param bagel
  */
class TradeActor(val trade: Trade, bagel: TheEverythingBagelDao) extends Actor
  with ActorLogging {

  import TradeActor._

  import scala.reflect.runtime.currentMirror

  implicit val akkaSystem = context.system

  private var status = trade.status

  private val dynamic = currentMirror.mkToolBox()


  override def preStart() = {

    status match {
      case TradeStatus.Pending =>
        context.actorOf(SimpleConditionActor.props(trade.buyConditions))
      case TradeStatus.Bought =>
        // TODO start the simple condition actors for sells, stop sells, etc
        //trade.stopLossConditions.map { sellConditions =>
        // send each conditional to its own actor to watch over
        // send messages back to this actor
        //          sellConditions.split(" or ").foreach { c =>
        //            if (c.contains("TrailingStop")) {
        //              // TODO regex pattern for simple price conditions
        //              val extractParams =
        //                """^.*?TrailingStop\((0\.\d{2}),\s(\d+\.\d+).*?""".r
        //              c match {
        //                case extractParams(percent, refPrice) =>
        //                  val trailStop = TrailingStop(trade.userId, trade.id, trade.info.marketName, percent.toDouble, refPrice.toDouble)
        //                  mediator ! Publish("TrailingStop", TrailingStop(trade.userId, trade.id, trade.info.marketName, percent.toDouble, refPrice.toDouble))
        //                          //              // assume simple conditions here
        //              trades.append(updatedTrade)
        //            }
        //          }
        val stopLoss = trade.stopLossConditions.getOrElse("")
        val stopProfit = trade.takeProfitConditions.getOrElse("")

      case _ =>
        log.warning(s"encountered a trade status of ${status}")
    }

    log.info(s"${trade.id} trade actor started")
  }

  override def postStop() = {

  }

  def receive: Receive = {
    case update: MarketUpdate =>
      context.system.actorSelection(s"${self.path}/*") ! update

    case UpdateTrade(request, Some(sender)) =>
      // TODO
    //updateTrade(user, tradeId, request, sender)

    case DeleteTrade(Some(sender)) =>
      // TODO
      deleteTrade(sender)

    case Buy(price) =>
      executeBuy(price)

    case Sell(price) =>

    case x =>
      log.warning(s"received unknown message - $x")
  }


  private def executeBuy(price: Double) = {
    log.info(s"buy ${trade.baseQuantity} ${trade.info.marketName} for user: ${trade.userId}")

    val updatedTrade = trade.copy(
      stat = TradeStat(
        buyPrice = Some(price),
        buyTime = Some(Instant.now().atOffset(ZoneOffset.UTC))),
      status = TradeStatus.Bought
    )

    // TODO don't assume this update succeeded
    bagel.updateTrade(updatedTrade)

    // TODO next we need to activate the actors for the sell conditions here
    //trade.stopLossConditions.map { sellConditions =>
      // send each conditional to its own actor to watch over
      // send messages back to this actor
      //          sellConditions.split(" or ").foreach { c =>
      //            if (c.contains("TrailingStop")) {
      //              // TODO regex pattern for simple price conditions
      //              val extractParams =
      //                """^.*?TrailingStop\((0\.\d{2}),\s(\d+\.\d+).*?""".r
      //              c match {
      //                case extractParams(percent, refPrice) =>
      //                  val trailStop = TrailingStop(trade.userId, trade.id, trade.info.marketName, percent.toDouble, refPrice.toDouble)
      //                  mediator ! Publish("TrailingStop", TrailingStop(trade.userId, trade.id, trade.info.marketName, percent.toDouble, refPrice.toDouble))
      //                          //              // assume simple conditions here
      //              trades.append(updatedTrade)
      //            }
      //          }

    val stopLoss = trade.stopLossConditions.getOrElse("")
    val stopProfit = trade.takeProfitConditions.getOrElse("")
  }

  private def executeSell(price: Double) = {
    log.info(s"sell ${trade.info.marketName} for user: ${trade.userId}")
    val updatedTrade = trade.copy(
      stat = TradeStat(
        sellPrice = Some(price),
        sellTime = Some(Instant.now().atOffset(ZoneOffset.UTC))),
      status = TradeStatus.Sold
    )

    bagel.updateTrade(updatedTrade).map { updated =>
      // this trade is finished kill this actor
      self ! PoisonPill
    }
  }

  private def deleteTrade(sender: ActorRef) = {
    status match {
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
              bagel
                .updateBalance(balance.copy(availableBalance = balance.availableBalance + deleted.baseQuantity))
                .map { x =>
                  sender ! Some(deleted)
                  self ! PoisonPill
                }

            case _ =>
              sender ! None
          }
        }

      case _ =>
        // all other trades statuses are cancellable
        bagel.updateTrade(trade.copy(status = TradeStatus.Cancelled)).map (sender ! _)
        self ! PoisonPill
    }
  }
}


