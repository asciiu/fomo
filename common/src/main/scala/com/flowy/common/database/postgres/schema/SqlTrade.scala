package com.flowy.common.database.postgres.schema

import com.flowy.common.utils.sql.SqlDatabase
import com.flowy.common.models._
import java.time.OffsetDateTime
import java.util.UUID


trait SqlTrade {
  protected val database: SqlDatabase

  import database._
  import com.flowy.common.slick.MyPostgresDriver.api._

  implicit val tradeStatusMapper = MappedColumnType.base[TradeStatus.Value, String](
    { os => os.toString }, { str => TradeStatus.withName(str) }
  )

  protected val trades = TableQuery[Trades]

  class Trades(tag: Tag) extends Table[Trade](tag, "trades") {
    def id = column[UUID]("id", O.PrimaryKey)
    def userId = column[UUID]("user_id")
    def apiKeyId = column[UUID]("user_api_key_id")
    def exchangeName = column[String]("exchange_name")
    def marketName = column[String]("market_name")
    def marketCurrencyAbbrev = column[String]("market_currency")
    def marketCurrencyName = column[String]("market_currency_long")
    def baseCurrencyAbbrev = column[String]("base_currency")
    def baseCurrencyName = column[String]("base_currency_long")
    def bQuantity = column[Double]("base_quantity")
    def mQuantity = column[Double]("currency_quantity")
    def status = column[TradeStatus.Value]("status")
    def createdOn = column[OffsetDateTime]("created_on")
    def updatedOn = column[OffsetDateTime]("updated_on")
    def buyTime = column[OffsetDateTime]("bought_time")
    def buyPrice = column[Double]("bought_price")
    def buyCondition = column[String]("bought_condition")
    def sellTime = column[OffsetDateTime]("sold_time")
    def sellPrice = column[Double]("sold_price")
    def sellCondition = column[String]("sold_condition")
    def buyConditions = column[String]("buy_condition")
    def stopLossConditions = column[String]("stop_loss_condition")
    def takeProfitConditions = column[String]("profit_condition")

    def marketInfoColumns = (
      exchangeName,
      marketName,
      marketCurrencyAbbrev,
      marketCurrencyName,
      baseCurrencyAbbrev,
      baseCurrencyName) <>((MarketInfo.apply _).tupled, MarketInfo.unapply)

    def tradeStatColumns = (
      buyTime.?,
      buyPrice.?,
      buyCondition.?,
      sellTime.?,
      sellPrice.?,
      sellCondition.?,
      mQuantity.?) <>((TradeStat.apply _).tupled, TradeStat.unapply)

    def * = (id,
      userId,
      apiKeyId,
      marketInfoColumns,
      tradeStatColumns,
      bQuantity,
      status,
      createdOn,
      updatedOn,
      buyConditions,
      stopLossConditions.?,
      takeProfitConditions.?) <>
      ((Trade.apply _).tupled, Trade.unapply)
   }
}
