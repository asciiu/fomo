package com.flow.marketmaker.services

import java.time.{Instant, ZoneOffset}

import akka.actor.{Actor, ActorLogging, Props}
import com.flow.marketmaker.database.postgres.SqlTheEverythingBagelDao
import com.flow.marketmaker.database.redis.OrderRepository
import com.flow.marketmaker.models.MarketStructures.MarketUpdate
import com.flow.marketmaker.models._
import com.softwaremill.bootzooka.common.sql.SqlDatabase
import models.BasicUserData

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import spray.json.DefaultJsonProtocol._
import redis.RedisClient

object MarketService {
  def props(marketName: String, sqlDatabase: SqlDatabase, redis: RedisClient)(implicit context: ExecutionContext) =
    Props(new MarketService(marketName, sqlDatabase, redis))

  case object ReturnAllData
  case object ReturnLatestMessage

  case class CreateOrder(forUser: BasicUserData, buyOrder: BuyOrder)
}


class MarketService(val marketName: String, sqlDatabase: SqlDatabase, redis: RedisClient) extends Actor
  with ActorLogging {

  import MarketService._
  lazy val bagel = new SqlTheEverythingBagelDao(sqlDatabase)
  implicit val akkaSystem = context.system

  implicit val cond  = jsonFormat4(BuyCondition)
  //implicit val order = jsonFormat5(BuyOrder)

  val orderRepo = new OrderRepository(redis)
  // TODO need a better collection here
  val orders = collection.mutable.ListBuffer[Order]()

  def receive: Receive = {
    case update: MarketUpdate =>
      updateState(update)

    case CreateOrder(user, buyOrder) =>
      createOrder(user, buyOrder)
  }


  private def updateState(update: MarketUpdate) = {
    val lastPrice = update.Last

//    val executeOrders = orders.filter(_.isCondition(lastPrice))
//
//    executeOrders.foreach{ order =>
//      println(s"Last Price: ${lastPrice}")
//      println(s"Execute ${order.orderType} ${order.quantity} ${order.marketName} for user: ${order.userId}")
//      val completedCondition = order.getCondition(lastPrice).getOrElse("null")
//      val updatedOrder = order.copy(
//        priceActual = Some(lastPrice),
//        completedTime = Some(Instant.now().atOffset(ZoneOffset.UTC)),
//        completedCondition = Some(completedCondition),
//        status = OrderStatus.Completed
//      )
//
//      //orderRepo.update(updatedOrder)
//      bagel.update(updatedOrder)
//    }
//
//    // remove the executed orders
//    orders --= executeOrders
  }


  private def createOrder(user: BasicUserData, newOrder: BuyOrder) = {
    bagel.insert(Order.fromBuyOrder(newOrder, user.id)).map (o => orders.append(o))

    bagel.findAllByUserId(user.id).map { orders => orders.foreach(println) }
    //orderRepo.add(Order.fromBuyOrder(newOrder, user.id)).map (order => orders.append(order))
  }
}

