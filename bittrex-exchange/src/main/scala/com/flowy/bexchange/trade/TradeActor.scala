package com.flowy.bexchange.trade

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models.MarketStructures.MarketUpdate
import com.flowy.common.models._
import java.time.{Instant, ZoneOffset}
import java.util.UUID

import com.flowy.bexchange.trade.TrailingStopLossActor.TrailingStop

import scala.collection.mutable.ListBuffer
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
class TradeActor(trade: Trade, bagel: TheEverythingBagelDao) extends Actor
  with ActorLogging {

  import TradeActor._

  import scala.reflect.runtime.currentMirror

  implicit val akkaSystem = context.system

  private var myTrade = trade
  private var status = trade.status

  private val dynamic = currentMirror.mkToolBox()

  private val buyConditions = scala.collection.mutable.ListBuffer[ActorRef]()
  private val lossConditions = scala.collection.mutable.ListBuffer[ActorRef]()
  private val profitConditions = scala.collection.mutable.ListBuffer[ActorRef]()

  override def preStart() = {
    loadConditions()
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
    log.info(s"buy ${myTrade.baseQuantity} ${myTrade.info.marketName} for user: ${myTrade.userId}")
    status = TradeStatus.Bought

    val updatedTrade = myTrade.copy(
      stat = TradeStat(
        boughtCondition = Some(condition),
        boughtPrice = Some(price),
        boughtTime = Some(Instant.now().atOffset(ZoneOffset.UTC))),
      status = TradeStatus.Bought
    )

    myTrade = updatedTrade

    bagel.updateTrade(updatedTrade).map(_ => loadConditions() )
  }

  private def loadConditions() = {
    def parseConditions(conditions: String, actorBuffer: ListBuffer[ActorRef]) = {
      conditions.split(" or ").foreach { cond =>
        val extractParams = """^.*?TrailingStop\((0\.\d{2,}),\s(\d+\.\d+).*?""".r
        cond match {
          case extractParams(percent, refPrice) =>
            val trail = TrailingStop(trade.userId, trade.id, trade.info.marketName, percent.toDouble, refPrice.toDouble)
            actorBuffer += context.actorOf(TrailingStopLossActor.props(TradeAction.Sell, trail), "TrailingStop")
          case sellConditions =>
            actorBuffer += context.actorOf(SimpleConditionActor.props(TradeAction.Sell, sellConditions), "SimpleCondition")
        }
      }
    }

    // this actor can start a trade from any status
    myTrade.status match {
      case TradeStatus.Pending =>
        // pending trade must monitor buy conditions
        buyConditions += context.actorOf(SimpleConditionActor.props(TradeAction.Buy, myTrade.buyCondition))

      case TradeStatus.Bought =>
        // bought trade must monitor sell conditions
        val stopLoss = myTrade.stopLossCondition.getOrElse("")
        if (stopLoss != "") {
          parseConditions(stopLoss, lossConditions)
        }

        val stopProfit = myTrade.profitCondition.getOrElse("")
        if (stopProfit != "") {
          parseConditions(stopProfit, profitConditions)
        }
      case _ =>
        log.warning(s"encountered a trade status of ${status}")
    }
  }

  private def executeSell(price: Double, condition: String) = {
    // TODO execute sell logic here

    log.info(s"sell ${myTrade.info.marketName} condition: $condition for user: ${myTrade.userId}")
    val updatedTrade = myTrade.copy(
      stat = TradeStat(
        soldCondition = Some(condition),
        soldPrice = Some(price),
        soldTime = Some(Instant.now().atOffset(ZoneOffset.UTC))),
      status = TradeStatus.Sold
    )

    myTrade = updatedTrade

    bagel.updateTrade(updatedTrade).map { updated =>
      // this trade is finished kill this actor
      self ! PoisonPill
    }
  }

  private def cancelTrade(sender: ActorRef) = {
    // first stop all children condition
    // stop previous conditions
    lossConditions.foreach ( a => context stop a)
    profitConditions.foreach ( a => context stop a)
    buyConditions.foreach (a => context stop a)

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
      // stop previous conditions
      buyConditions.foreach ( a => context stop a)

      bagel.updateTrade(
        trade.copy(
          baseQuantity = request.baseQuantity,
          buyCondition = request.buyCondition,
          stopLossCondition = request.stopLossCondition,
          profitCondition = request.profitCondition)
      ).map { updated =>
        if (updated.isDefined) {
          myTrade = updated.get
          loadConditions()
        }
        sender ! updated
      }


    } else if (status == TradeStatus.Bought) {
      // stop previous conditions
      profitConditions.foreach ( a => context stop a)
      buyConditions.foreach (a => context stop a)

      bagel.updateTrade(
        trade.copy(
          stopLossCondition = request.stopLossCondition,
          profitCondition = request.profitCondition)
      ).map { updated =>

        if (updated.isDefined) {
          myTrade = updated.get
          loadConditions()
        }

        sender ! updated
      }

    } else {
      sender ! None
    }
  }
}


