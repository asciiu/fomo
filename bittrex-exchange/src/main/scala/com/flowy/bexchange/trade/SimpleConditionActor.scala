package com.flowy.bexchange.trade

import akka.actor.{Actor, ActorLogging, Props, PoisonPill}
import com.flowy.common.models.MarketStructures.MarketUpdate
import scala.concurrent.{ExecutionContext}
import scala.tools.reflect.ToolBox


object SimpleConditionActor {
  def props(condition: String)(implicit context: ExecutionContext) =
    Props(new SimpleConditionActor(condition))

  case class UpdateCondition(condition: String)
}


/**
  * Keeps track of simple conditions based upon an expression e.g. (price > 0.999)
  * @param condition
  */
class SimpleConditionActor(condition: String) extends Actor
  with ActorLogging {

  import TradeActor._

  import SimpleConditionActor._
  import scala.reflect.runtime.currentMirror

  private var expression = condition
  implicit val akkaSystem = context.system

  private val dynamic = currentMirror.mkToolBox()

  def receive: Receive = {
    case update: MarketUpdate =>
      val lastPrice = update.Last

      val expString = expression.replace("price", lastPrice.toString)

      // if true tell parent to buy and exit
      if (dynamic.eval(dynamic.parse(s"$expString")) == true) {
        context.parent ! Buy(lastPrice, expString)
        self ! PoisonPill
      }

    case UpdateCondition(newCondition) =>
      expression = condition

    case x =>
      log.warning(s"received unknown message - $x")
  }
}




