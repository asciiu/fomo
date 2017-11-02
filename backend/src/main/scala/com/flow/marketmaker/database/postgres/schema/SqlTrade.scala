package com.flow.marketmaker.database.postgres.schema

import com.flow.marketmaker.models.{Order, OrderStatus, OrderType, Trade}
import com.softwaremill.bootzooka.common.sql.SqlDatabase
import java.time.OffsetDateTime
import java.util.UUID

import spray.json.JsValue


trait SqlTrade {
  protected val database: SqlDatabase

  import database._
  import slick.MyPostgresDriver.api._

  implicit val resourceTypeTypeMapper = MappedColumnType.base[OrderType.Value, String](
    { ot => ot.toString }, { str => OrderType.withName(str) }
  )

  implicit val resourceTypeStatusMapper = MappedColumnType.base[OrderStatus.Value, String](
    { os => os.toString }, { str => OrderStatus.withName(str) }
  )

  protected val trades = TableQuery[Trades]

  class Trades(tag: Tag) extends Table[Trade](tag, "trades") {
    def id = column[UUID]("id", O.PrimaryKey)

    def userId = column[UUID]("user_id")

    def openOrderId = column[UUID]("open_order_id")

    def closeOrderId = column[UUID]("close_order_id")

    def exchangeName = column[String]("exchange_name")

    def marketCurrency = column[String]("market_currency_short_name")

    def marketCurrencyLong = column[String]("market_currency_long_name ")

    def boughtPrice = column[Double]("bought_price")

    def boughTime = column[OffsetDateTime]("bought_time")

    def soldPrice = column[Double]("sold_price")

    def soldTime = column[OffsetDateTime]("sold_time")

    def * = (id,
      userId,
      openOrderId,
      closeOrderId.?,
      exchangeName,
      marketCurrency,
      marketCurrencyLong,
      boughtPrice.?,
      boughTime.?,
      soldPrice.?,
      soldTime.?) <>
      ((Trade.apply _).tupled, Trade.unapply)
  }
}
