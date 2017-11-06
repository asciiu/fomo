package com.flow.marketmaker.database.redis

import akka.actor.ActorSystem
import akka.util.ByteString
import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

import com.flow.marketmaker.models._
import io.circe.Json
import redis.RedisClient
import spray.json.JsNull

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

/**
  * Class responsible for managing com.flow.marketmaker.models.Order instances in a Redis Database.
  *
  * @param actorSystem Implicit [[ActorSystem]]. Required by rediscala client.
  */
class OrderRepository(redis: RedisClient)(implicit actorSystem: ActorSystem) {

  /**
    * Client holding a connection towards Redis Database server.
    */
  //val redis = RedisClient()

  /**
    * Responsible for persisting a Order. It first finds out the next ID to be used,
    * sets it in the order and then persists it to the DB.
    *
    * @param order The Order to be persisted.
    * @return A [[Future]] with the Order persisted, including his ID.
    */
  def add(order: Order): Future[Order] = {
    val createdTime = Instant.now().atOffset(ZoneOffset.UTC)
    val status = OrderStatus.Pending
    val newOrder = order.copy(createdTime = createdTime, status = status)
    val key = getOrderKey(order.id)
    val futureAdded =
      redis.hmset(key,
        Map("id" -> order.id.toString,
          "userId" -> order.userId.toString,
          "exchangeName" -> order.exchangeName,
          "marketName" -> order.marketName,
          "createdTime" -> createdTime.toString,
          "completedTime" -> "",
          "completedCondition" -> "",
          "priceActual" -> "",
          "quantity" -> order.quantity.toString,
          "orderType" -> order.orderType.toString,
          "status" -> status.toString
        ))

    futureAdded map { res => newOrder }
  }


  /**
    * Must only use to update an existing order because ortherwise order.id.get
    * on a None id will throw exception.
    * @param order
    * @return
    */
  def update(order: Order): Future[Order] = {
    val key = getOrderKey(order.id)
    val futureAdded =
      redis.hmset(key,
        Map(
          "completedTime" -> order.completedTime.getOrElse("").toString,
          "completedCondition" -> order.completedCondition.getOrElse(""),
          "priceActual" -> order.priceActual.getOrElse(0.0).toString,
          "status" -> order.status.toString
        ))

    futureAdded map { res => order }

  }

  /**
    * @param id The ID of the Order to be found.
    * @return A [[Future]] with an [[Option]] holding the Order found or None.
    */
  def find(id: UUID): Future[Option[Order]] = {
    redis.hgetall(getOrderKey(id)) map { keysAndValues =>
      if (keysAndValues.isEmpty) None else Some(mapToOrder(keysAndValues))
    }
  }

  def remove(id: UUID): Future[Boolean] = {
    redis.del(getOrderKey(id)) map { rowsDeleted =>
      rowsDeleted == 1
    }
  }

  private def getNextId(): Future[Long] = {
    redis.incr("next_order_id")
  }

  private def mapToOrder(keysAndValues: Map[String, ByteString]): Order = {
    val completedTime = if (keysAndValues("completedTime") == "") None else Some(OffsetDateTime.parse(keysAndValues("completedTime").utf8String))
    val priceActual = if (keysAndValues("priceActual") == "") None else Some(keysAndValues("priceActual").utf8String.toDouble)
    val condition = if (keysAndValues("completedCondition") == "") None else Some(keysAndValues("completedCondition").utf8String )

    Order(
      UUID.fromString(keysAndValues("id").utf8String),
      UUID.fromString(keysAndValues("userId").utf8String),
      keysAndValues("exchangeName").utf8String,
      keysAndValues("marketName").utf8String,
      OffsetDateTime.parse(keysAndValues("createdTime").utf8String),
      completedTime,
      condition,
      priceActual,
      keysAndValues("quantity").utf8String.toDouble,
      OrderType.withName(keysAndValues("orderType").utf8String),
      OrderStatus.withName(keysAndValues("status").utf8String),
      Json.Null
    )
  }

  private def getOrderKey(id: UUID): String = s"Order:$id"

}

