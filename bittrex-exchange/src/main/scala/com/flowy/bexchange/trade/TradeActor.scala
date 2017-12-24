package com.flowy.bexchange.trade

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models.MarketStructures.MarketUpdate
import com.flowy.common.models._
import java.time.{Instant, ZoneOffset}
import java.util.UUID

import com.flowy.bexchange.trade.TrailingStopLossActor.TrailingStop

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.tools.reflect.ToolBox


object TradeActor {
  def props(trade: Trade, bagel: TheEverythingBagelDao)(implicit context: ExecutionContext) =
    Props(new TradeActor(trade, bagel))

  case class Trigger(action: TradeAction.Value, price: Double, condition: String)
  case class Buy(price: Double, atCondition: String)
  case class Sell(price: Double, atCondition: String)
  case class Cancel(sender: ActorRef)
  case class Update(user: UserData, request: TradeRequest, sender: ActorRef)
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

    // this actor can start a trade from any status
    status match {
      case TradeStatus.Pending =>
        // pending trade must monitor buy conditions
        context.actorOf(SimpleConditionActor.props(TradeAction.Buy, trade.buyCondition))
      case TradeStatus.Bought =>
        // bought trade must monitor sell conditions
        loadSellConditions()
      case _ =>
        log.warning(s"encountered a trade status of ${status}")
    }
  }

  override def postStop() = {
    log.info(s"trade ${trade.id} is shutting down")
  }

  def receive: Receive = {
    case update: MarketUpdate =>
      context.system.actorSelection(s"${self.path}/*") ! update

    case Update(user, request, sender) =>
      updateTrade(user, request, sender)

    case Cancel(sender) =>
      cancelTrade(sender)

    case Trigger(TradeAction.Buy, price, condition) =>
      executeBuy(price, condition)

    case Trigger(TradeAction.Sell, price, condition) =>
      executeSell(price, condition)

    case x =>
      log.warning(s"received unknown message - $x")
  }


  private def executeBuy(price: Double, condition: String) = {
    log.info(s"buy ${trade.baseQuantity} ${trade.info.marketName} for user: ${trade.userId}")
    status = TradeStatus.Bought

    val updatedTrade = trade.copy(
      stat = TradeStat(
        boughtCondition = Some(condition),
        boughtPrice = Some(price),
        boughtTime = Some(Instant.now().atOffset(ZoneOffset.UTC))),
      status = TradeStatus.Bought
    )

    bagel.updateTrade(updatedTrade).map(_ => loadSellConditions() )
  }

  private def loadSellConditions() = {
    def parseConditions(conditions: String) = {
      conditions.split(" or ").foreach { cond =>
        val extractParams = """^.*?TrailingStop\((0\.\d{2,}),\s(\d+\.\d+).*?""".r
        cond match {
          case extractParams(percent, refPrice) =>
            val trail = TrailingStop(trade.userId, trade.id, trade.info.marketName, percent.toDouble, refPrice.toDouble)
            context.actorOf(TrailingStopLossActor.props(TradeAction.Sell, trail), "TrailingStop")
          case sellConditions =>
            context.actorOf(SimpleConditionActor.props(TradeAction.Sell, sellConditions), "SimpleCondition")
        }
      }
    }

    val stopLoss = trade.stopLossCondition.getOrElse("")
    if (stopLoss != "") {
      parseConditions(stopLoss)
    }

    val stopProfit = trade.profitCondition.getOrElse("")
    if (stopProfit != "") {
       parseConditions(stopProfit)
    }
  }

  private def executeSell(price: Double, condition: String) = {
    // TODO execute sell logic here

    log.info(s"sell ${trade.info.marketName} condition: $condition for user: ${trade.userId}")
    val updatedTrade = trade.copy(
      stat = TradeStat(
        soldCondition = Some(condition),
        soldPrice = Some(price),
        soldTime = Some(Instant.now().atOffset(ZoneOffset.UTC))),
      status = TradeStatus.Sold
    )

    bagel.updateTrade(updatedTrade).map { updated =>
      // this trade is finished kill this actor
      self ! PoisonPill
    }
  }

  private def cancelTrade(sender: ActorRef) = {
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

  private def updateTrade(userData: UserData, request: TradeRequest, sender: ActorRef) = {
    // users can only update their own trades
    if (userData.id != trade.userId) {
      sender ! None
    } else if (status == TradeStatus.Pending) {

      bagel.updateTrade(
        trade.copy(
          baseQuantity = request.baseQuantity,
          buyCondition = request.buyCondition,
          stopLossCondition = request.stopLossCondition,
          profitCondition = request.profitCondition)
      ).map { updated =>
        sender ! updated
      }

    } else if (status == TradeStatus.Bought) {

      bagel.updateTrade(
        trade.copy(
          stopLossCondition = request.stopLossCondition,
          profitCondition = request.profitCondition)
      ).map { updated =>
        sender ! updated
      }

    } else {
      sender ! None
    }
  }
}

