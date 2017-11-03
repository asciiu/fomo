package com.flow.marketmaker.services


import java.time.{Instant, ZoneOffset}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.flow.marketmaker.database.postgres.SqlTheEverythingBagelDao
import com.flow.marketmaker.database.redis.OrderRepository
import com.flow.marketmaker.models.MarketStructures.MarketUpdate
import com.flow.marketmaker.models._
import models.BasicUserData

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import redis.RedisClient


object MarketService {
  def props(marketName: String, bagel: SqlTheEverythingBagelDao, redis: RedisClient)(implicit context: ExecutionContext) =
    Props(new MarketService(marketName, bagel, redis))

  case object ReturnAllData
  case object ReturnLatestMessage

  case class CreateOrder(forUser: BasicUserData, buyOrder: BuyOrder)
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
class MarketService(val marketName: String, bagel: SqlTheEverythingBagelDao, redis: RedisClient) extends Actor
  with ActorLogging {

  import MarketService._

  implicit val akkaSystem = context.system

  val orderRepo = new OrderRepository(redis)
  val conditions = collection.mutable.ListBuffer[SimpleConditional]()

  override def preStart() = {
    // load pending conditions from bagel
    bagel.findAllByOrderStatus(marketName, OrderStatus.Pending).map { orders =>
      // map all the conditions into a single collection
      val pendingConditions = orders.map( o => JsonConditionTranslator.fromOrder(o) ).flatten

      if (pendingConditions.length > 0) {
        log.info(s"$marketName loading pending orders: $pendingConditions")
        conditions.append(pendingConditions: _*)
      }
    }
  }

  def receive: Receive = {
    case update: MarketUpdate =>
      updateState(update)

    case CreateOrder(user, buyOrder) =>
      createOrder(user, buyOrder)

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

    processConditions.foreach { condish =>
      val orderId = condish.orderId

      bagel.findByOrderId(orderId).map { order =>
        if (order.isDefined) {
          // TODO this is where the order shall be executed via the BittrexClient
          // read the actual price from Bittrex and set the priceActual below

          val updatedOrder = order.get.copy(
            priceActual = Some(lastPrice),
            completedTime = Some(Instant.now().atOffset(ZoneOffset.UTC)),
            completedCondition = Some(condish.toString),
            status = OrderStatus.Completed
          )

          println(s"Last Price: ${lastPrice}")
          println(s"Execute $orderId ${updatedOrder.quantity} ${updatedOrder.marketName} for user: ${updatedOrder.userId}")
          bagel.update(updatedOrder)
        }
      }
    }

    // remove the conditions that have passed
    conditions --= processConditions
  }

  /**
    * Creates a new order and appends the order conditions to state.
    * On each update the conditions will be checked
    * @param user
    * @param newOrder
    * @return
    */
  private def createOrder(user: BasicUserData, newOrder: BuyOrder) = {
    // TODO make a new order if there is not enough balance to do so
    bagel.insert(Order.fromBuyOrder(newOrder, user.id)).map { o =>
      // the : _* is the splat operator
      conditions.append(JsonConditionTranslator.fromOrder(o): _*)
    }
  }

  private def postTrade(user: BasicUserData, request: TradeRequest, sender: ActorRef) = {
    println(user)
    println(request)

    sender ! true
  }
}

