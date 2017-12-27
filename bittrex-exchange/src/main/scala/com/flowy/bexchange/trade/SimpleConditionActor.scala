package com.flowy.bexchange.trade

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import com.flowy.common.models.MarketStructures.MarketUpdate
import com.flowy.common.models.TradeAction

import scala.concurrent.ExecutionContext
import scala.tools.reflect.ToolBox


object SimpleConditionActor {
  def props(action: TradeAction.Value, condition: String, name: String)(implicit context: ExecutionContext) =
    Props(new SimpleConditionActor(action, condition, name))

  case class UpdateCondition(condition: String)
}


/**
  * Keeps track of simple conditions based upon an expression e.g. (price > 0.999)
  * @param condition
  */
class SimpleConditionActor(action: TradeAction.Value, condition: String, name: String) extends Actor
  with ActorLogging {

  import TradeActor._

  import SimpleConditionActor._
  import scala.reflect.runtime.currentMirror

  private var expression = condition
  implicit val akkaSystem = context.system

  private val dynamic = currentMirror.mkToolBox()

  override def preStart = {
    log.info(s"$action $condition started")
  }

  def receive: Receive = {
    case update: MarketUpdate =>
      val lastPrice = update.Last

      val expString = expression.replace("price", lastPrice.toString)

      // if true tell parent to buy and exit
      if (dynamic.eval(dynamic.parse(s"$expString")) == true) {
        context.parent ! Trigger(action, lastPrice, expString, name)
        self ! PoisonPill
      }

    case UpdateCondition(newCondition) =>
      expression = condition

    case x =>
      log.warning(s"received unknown message - $x")
  }
}




