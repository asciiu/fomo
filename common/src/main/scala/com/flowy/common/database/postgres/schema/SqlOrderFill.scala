package com.flowy.common.database.postgres.schema

import java.time.OffsetDateTime
import java.util.UUID

import com.flowy.common.models._
import io.circe.Json

trait SqlOrderFill extends SqlSchema {

  import com.flowy.common.slick.MyPostgresDriver.api._
  import database._

  protected val orderFills = TableQuery[OrderFills]

  class OrderFills(tag: Tag) extends Table[OrderFill](tag, "order_fills") {
    def id = column[UUID]("id", O.PrimaryKey)
    def orderId = column[UUID]("order_id")
    def condition = column[Json]("condition")
    def price = column[BigDecimal]("price")
    def quantity = column[BigDecimal]("quantity")
    def createdOn = column[OffsetDateTime]("created_on")
    def updatedOn = column[OffsetDateTime]("updated_on")

    def * = (
      id,
      orderId,
      condition,
      price,
      quantity,
      createdOn,
      updatedOn
      ) <>
      ((OrderFill.apply _).tupled, OrderFill.unapply)
   }
}
