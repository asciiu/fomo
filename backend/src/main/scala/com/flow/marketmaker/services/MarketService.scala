package com.flow.marketmaker.services

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.flow.marketmaker.database.TheEverythingBagelDao
import com.flow.marketmaker.models.MarketStructures.MarketUpdate
import com.flow.marketmaker.models._
import models.BasicUserData
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import redis.RedisClient


object MarketService {
  def props(marketName: String, bagel: TheEverythingBagelDao, redis: RedisClient)(implicit context: ExecutionContext) =
    Props(new MarketService(marketName, bagel, redis))

  case class PostTrade(forUser: BasicUserData, request: TradeRequest, sender: Option[ActorRef] = None)

}


/**
  * TODO need to rename this to the SimpleCondtionalService
  * note: This service shall be started when the system starts
  * currently it is started when an update in the supervisor gets a market update
  * @param marketName
  * @param bagel
  * @param redis
  */
class MarketService(val marketName: String, bagel: TheEverythingBagelDao, redis: RedisClient) extends Actor
  with ActorLogging {

  import MarketService._

  implicit val akkaSystem = context.system

  val conditions = collection.mutable.ListBuffer[SimpleConditional]()

  override def preStart() = {
    // load pending conditions from bagel
    //bagel.findAllByOrderStatus(marketName, OrderStatus.Pending).map { orders =>
    //  // map all the conditions into a single collection
    //  val pendingConditions = orders.map( o => JsonConditionTranslator.fromOrder(o) ).flatten

    //  if (pendingConditions.length > 0) {
    //    log.info(s"$marketName loading pending orders: $pendingConditions")
    //    conditions.append(pendingConditions: _*)
    //  }
    //}

    log.info(s"$marketName actor started")
  }

  def receive: Receive = {
    case update: MarketUpdate =>
      updateState(update)

    case PostTrade(user, request, Some(sender)) =>
      postTrade(user, request, sender)
  }

  /**
    * Updates the internal state of this service.
    * @param update
    * @return
    */
  private def updateState(update: MarketUpdate) = {
    val lastPrice = update.Last

    val processConditions = conditions.filter(_.evaluate(lastPrice))

//    processConditions.foreach { condish =>
//      val orderId = condish.orderId
//
//      bagel.findByOrderId(orderId).map { order =>
//        if (order.isDefined) {
//          // TODO this is where the order shall be executed via the BittrexClient
//          // read the actual price from Bittrex and set the priceActual below
//
//          val updatedOrder = order.get.copy(
//            priceActual = Some(lastPrice),
//            completedTime = Some(Instant.now().atOffset(ZoneOffset.UTC)),
//            completedCondition = Some(condish.toString),
//            status = OrderStatus.Completed
//          )
//
//          println(s"Last Price: ${lastPrice}")
//          println(s"Execute $orderId ${updatedOrder.quantity} ${updatedOrder.marketName} for user: ${updatedOrder.userId}")
//          bagel.update(updatedOrder)
//        }
//      }
//    }

    // remove the conditions that have passed
    conditions --= processConditions
  }


  private def postTrade(user: BasicUserData, request: TradeRequest, sender: ActorRef) = {
    val trade = Trade.fromRequest(request, user.id)

    bagel.insert(trade).map { result =>
      if (result > 0) {
        sender ! true
      } else {
        sender ! false
      }
    }
  }
}

