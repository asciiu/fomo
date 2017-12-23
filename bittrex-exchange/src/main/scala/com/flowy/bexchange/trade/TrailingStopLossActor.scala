package com.flowy.bexchange.trade

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import com.flowy.common.models.MarketStructures.MarketUpdate
import com.flowy.common.models.TradeAction
import java.util.UUID

import com.flowy.bexchange.trade.TrailingStopLossActor.TrailingStop

import scala.concurrent.ExecutionContext

object TrailingStopLossActor {
  def props(action: TradeAction.Value, trail: TrailingStop) (implicit context: ExecutionContext) =
    Props(new TrailingStopLossActor(action, trail))

  case class TrailingStop(userId: UUID, tradeId: UUID, marketName: String, percent: Double, refPrice: Double)
}

class TrailingStopLossActor(action: TradeAction.Value, trail: TrailingStop) extends Actor with ActorLogging{
  import TradeActor._

  private var ceilingPrice: Double = trail.refPrice

  override def preStart(): Unit = {
    log.info(s"TrailingStop(${trail.percent}, ${trail.refPrice})")
  }

  override def postStop(): Unit = {}

  def receive = {

    /**
      * update state of service from market update
      */
    case update: MarketUpdate =>
      updateState(update.Last)
  }

  /**
    * updates the trailing stop state
    * @param price last price
    * @return
    */
  private def updateState(price: Double) = {
    if (ceilingPrice < price) {
      log.info(s"new ceiling ${price}")
      ceilingPrice = price
    } else if (price <= triggerPrice) {
      log.info(s"current trigger ${triggerPrice}")
      context.parent ! Trigger(action, price, "TrailingStop")
      self ! PoisonPill
    }
  }

  private def triggerPrice: Double = ceilingPrice * (1.0 - trail.percent)
}
