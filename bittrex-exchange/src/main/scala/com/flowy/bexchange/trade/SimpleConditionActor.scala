package com.flowy.bexchange.trade

import java.time.{Instant, ZoneOffset}
import akka.actor.{Actor, ActorLogging, ActorRef, Props, PoisonPill}
import com.flowy.common.models.MarketStructures.MarketUpdate
import com.flowy.common.models._
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

  import SimpleConditionActor._
  import scala.reflect.runtime.currentMirror

  private var expression = condition
  implicit val akkaSystem = context.system

  private val dynamic = currentMirror.mkToolBox()

  def receive: Receive = {
    case update: MarketUpdate =>
      updateState(update)

    case UpdateCondition(newCondition) =>
      expression = condition

    case x =>
      log.warning(s"received unknown message - $x")
  }

  /**
    * Updates the internal state of this service.
    * @param update
    * @return
    */
  private def updateState(update: MarketUpdate) = {
    val lastPrice = update.Last

    val expString = expression.replace("price", lastPrice.toString)

    if (dynamic.eval(dynamic.parse(s"$expString")) == true) {
      context.parent ! expString
    }
  }
}




