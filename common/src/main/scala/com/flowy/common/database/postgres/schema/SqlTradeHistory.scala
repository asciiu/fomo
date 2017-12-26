package com.flowy.common.database.postgres.schema

import java.time.OffsetDateTime
import java.util.UUID

import com.flowy.common.models._
import com.flowy.common.utils.sql.SqlDatabase


trait SqlTradeHistory {
  protected val database: SqlDatabase

  import com.flowy.common.slick.MyPostgresDriver.api._
  import database._

  implicit val tradeActMapper = MappedColumnType.base[TradeAction.Value, String](
    { os => os.toString }, { str => TradeAction.withName(str) }
  )

  protected val tradeHistory = TableQuery[TradeEvents]

  class TradeEvents(tag: Tag) extends Table[TradeHistory](tag, "trade_history") {
    def id = column[UUID]("id", O.PrimaryKey)
    def userId = column[UUID]("user_id")
    def tradeId = column[UUID]("trade_id")
    def exchangeName = column[String]("exchange_name")
    def marketName = column[String]("market_name")
    def currency = column[String]("currency")
    def currencyLong = column[String]("currency_long")
    def currencyQty = column[BigDecimal]("currency_quantity")
    def baseCurrency = column[String]("base_currency")
    def baseCurrencyLong = column[String]("base_currency_long")
    def baseQty = column[BigDecimal]("base_quantity")
    def action = column[TradeAction.Value]("type")
    def bidAsk = column[BigDecimal]("bid_ask_price")
    def actual = column[BigDecimal]("actual_price")
    def title = column[String]("title")
    def summary = column[String]("summary")
    def createdOn = column[OffsetDateTime]("created_on")
    def updatedOn = column[OffsetDateTime]("updated_on")

    def * = (id,
      userId,
      tradeId,
      exchangeName,
      marketName,
      currency,
      currencyLong,
      currencyQty,
      baseCurrency,
      baseCurrencyLong,
      baseQty,
      action,
      bidAsk,
      actual,
      title,
      summary,
      createdOn,
      updatedOn
    ) <>
      ((TradeHistory.apply _).tupled, TradeHistory.unapply)
   }
}
