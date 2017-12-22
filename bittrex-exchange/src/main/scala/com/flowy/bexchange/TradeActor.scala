package com.flowy.bexchange

import java.time.{Instant, ZoneOffset}
import akka.actor.{Actor, ActorLogging, ActorRef, Props, PoisonPill}
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models.MarketStructures.MarketUpdate
import com.flowy.common.models._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext}
import scala.tools.reflect.ToolBox


object TradeActor {
  def props(trade: Trade, bagel: TheEverythingBagelDao)(implicit context: ExecutionContext) =
    Props(new TradeActor(trade, bagel))

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
    log.info(s"${trade.id} trade actor started")
  }

  override def postStop() = {

  }

  def receive: Receive = {
    case update: MarketUpdate =>
      updateState(update)

    case UpdateTrade(request, Some(sender)) =>
      //updateTrade(user, tradeId, request, sender)

    case DeleteTrade(Some(sender)) =>
      deleteTrade(sender)

    case x =>
      log.warning(s"received unknown message - $x")
  }

  /**
    * Updates the internal state of this service.
    * @param update
    * @return
    */
  private def updateState(update: MarketUpdate) = {
    println(update)
    val lastPrice = update.Last

    status match {
      case TradeStatus.Pending =>
        val condition = trade.buyConditions.replace("price", lastPrice.toString)
        // if buy condition
        if (dynamic.eval(dynamic.parse(s"$condition")) == true) {

          log.info(s"buy ${trade.baseQuantity} ${trade.info.marketName} for user: ${trade.userId}")

          val updatedTrade = trade.copy(
            stat = TradeStat(
              buyPrice = Some(lastPrice),
              buyTime = Some(Instant.now().atOffset(ZoneOffset.UTC))),
            status = TradeStatus.Bought
          )

          // TODO don't assume this update succeeded
          bagel.updateTrade(updatedTrade)

        }

      case TradeStatus.Bought =>
        trade.stopLossConditions.map { sellConditions =>
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
          //                  log.info(s"sending trailing stop $trailStop")
          //                case _ =>
          //                  log.warning(s"Leo is sent in an invalid format for the TrailingStop got: $c should be TrailingStop(percent, startTopPriceAt)")
          //              }
          //            } else {
          //              // assume simple conditions here
          //              trades.append(updatedTrade)
          //            }
          //          }

          val stopLoss = trade.stopLossConditions.getOrElse("").replace("price", lastPrice.toString)
          val stopProfit = trade.takeProfitConditions.getOrElse("").replace("price", lastPrice.toString)

          if ((stopLoss != "" && dynamic.eval(dynamic.parse(s"$stopLoss")) == true) ||
            (stopProfit != "" && dynamic.eval(dynamic.parse(s"$stopProfit")) == true)) {

            // TODO this is where the order shall be executed via the BittrexClient

            // TODO the sellPrice should be the actual price you may need to read this from bittrex
            log.info(s"sell ${trade.info.marketName} for user: ${trade.userId}")
            val updatedTrade = trade.copy(
              stat = TradeStat(
                sellPrice = Some(lastPrice),
                sellTime = Some(Instant.now().atOffset(ZoneOffset.UTC))),
              status = TradeStatus.Sold
            )

            bagel.updateTrade(updatedTrade).map { updated =>
              // this trade is finished kill this actor
              self ! PoisonPill
            }
          }
        }
      case _ =>
        // ignore this condition as a Sold trade might be in the process of shutting down
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
//
//  private def updateTrade(user: UserData, tradeId: UUID, request: TradeRequest, sender: ActorRef) = {
//    bagel.findTradeById(tradeId).map {
//      // not permitted to change someone elses trade
//      case Some(trade) if trade.userId != user.id =>
//        sender ! None
//
//      // trade status pending update quantity and conditions
//      case Some(trade) if trade.status == TradeStatus.Pending =>
//        bagel.updateTrade(
//          trade.copy(
//            baseQuantity = request.baseQuantity,
//            buyConditions = request.buyConditions,
//            stopLossConditions = request.stopLossConditions,
//            takeProfitConditions = request.takeProfitConditions)
//        ).map { updated =>
//          sender ! updated
//        }
//
//      // trade status bought update sellconditions only
//      case Some(trade) if trade.status == TradeStatus.Bought =>
//        bagel.updateTrade(
//          trade.copy(
//            stopLossConditions = request.stopLossConditions,
//            takeProfitConditions = request.takeProfitConditions)
//        ).map { updated =>
//          sender ! updated
//        }
//
//      case None =>
//        sender ! None
//    }
//  }
}


