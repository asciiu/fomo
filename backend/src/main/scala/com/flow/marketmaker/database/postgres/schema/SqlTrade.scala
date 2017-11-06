package com.flow.marketmaker.database.postgres.schema

import com.flow.marketmaker.models._
import com.softwaremill.bootzooka.common.sql.SqlDatabase
import java.time.OffsetDateTime
import java.util.UUID

import io.circe.Json
import spray.json.JsValue


trait SqlTrade {
  protected val database: SqlDatabase

  import database._
  import slick.MyPostgresDriver.api._

  implicit val tradeStatusMapper = MappedColumnType.base[TradeStatus.Value, String](
    { os => os.toString }, { str => TradeStatus.withName(str) }
  )

  protected val trades = TableQuery[Trades]

  class Trades(tag: Tag) extends Table[Trade](tag, "trades") {
    def id = column[UUID]("id", O.PrimaryKey)
    def userId = column[UUID]("user_id")
    def exchangeName = column[String]("exchange_name")
    def marketName = column[String]("market_name")
    def marketCurrencyAbbrev = column[String]("market_currency_abbrev")
    def marketCurrencyName = column[String]("market_currency_name")
    def baseCurrencyAbbrev = column[String]("base_currency_abbrev")
    def baseCurrencyName = column[String]("base_currency_name")
    def quantity = column[Double]("quantity")
    def status = column[TradeStatus.Value]("status")
    def createdOn = column[OffsetDateTime]("created_on")
    def updatedOn = column[OffsetDateTime]("updated_on")
    def buyTime = column[OffsetDateTime]("buy_time")
    def buyPrice = column[Double]("buy_price")
    def buyCondition = column[String]("buy_condition")
    def buyConditions = column[Json]("buy_conditions")
    def sellTime = column[OffsetDateTime]("sell_time")
    def sellPrice = column[Double]("sell_price")
    def sellCondition = column[String]("sell_condition")
    def sellConditions = column[Json]("sell_conditions")

    def * = (id,
      userId,
      exchangeName,
      marketName,
      marketCurrencyAbbrev,
      marketCurrencyName,
      baseCurrencyAbbrev,
      baseCurrencyName,
      quantity,
      status,
      createdOn,
      updatedOn,
      buyTime.?,
      buyPrice.?,
      buyCondition.?,
      buyConditions,
      sellTime.?,
      sellPrice.?,
      sellCondition.?,
      sellConditions.?) <>
      ((Trade.apply _).tupled, Trade.unapply)
   }
}
