package com.flow.marketmaker.database.postgres.schema

import com.flow.marketmaker.models.{Order, OrderStatus, OrderType}
import com.softwaremill.bootzooka.common.sql.SqlDatabase
import java.time.OffsetDateTime
import java.util.UUID

import io.circe.Json
import spray.json.JsValue


trait SqlOrder {
  protected val database: SqlDatabase

  import database._
  import slick.MyPostgresDriver.api._

  implicit val orderTypeMapper = MappedColumnType.base[OrderType.Value, String] (
    { ot => ot.toString },
    { str => OrderType.withName(str) }
  )

  implicit val orderStatusMapper = MappedColumnType.base[OrderStatus.Value, String](
    { os => os.toString },
    { str => OrderStatus.withName(str) }
  )

  protected val orders = TableQuery[Orders]

  class Orders(tag: Tag) extends Table[Order](tag, "orders") {
    def id = column[UUID]("id", O.PrimaryKey)
    def userId = column[UUID]("user_id")
    def exchangeName = column[String]("exchange_name")
    def marketName = column[String]("market_name")
    def createdTime = column[OffsetDateTime]("created_time")
    def completedTime = column[OffsetDateTime]("completed_time")
    def completedCondition = column[String]("completed_condition")
    def priceActual = column[Double]("price_actual")
    def quantity = column[Double]("quantity")
    def orderType = column[OrderType.Value]("order_type")
    def status = column[OrderStatus.Value]("status")
    def json = column[Json]("conditions")

    def * = (id,
      userId,
      exchangeName,
      marketName,
      createdTime,
      completedTime.?,
      completedCondition.?,
      priceActual.?,
      quantity,
      orderType,
      status,
      json) <>
        ((Order.apply _).tupled, Order.unapply)
  }
}

