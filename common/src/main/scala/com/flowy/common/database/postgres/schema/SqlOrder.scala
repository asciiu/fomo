package com.flowy.common.database.postgres.schema

import java.time.OffsetDateTime
import java.util.UUID

import com.flowy.common.models._
import com.flowy.common.utils.sql.SqlDatabase
import io.circe.Json


trait SqlOrder extends SqlSchema {

  import com.flowy.common.slick.MyPostgresDriver.api._
  import database._

  implicit val orderStatusMapper = MappedColumnType.base[OrderStatus.Value, String](
    { os => os.toString }, { str => OrderStatus.withName(str) }
  )

  implicit val orderTypeMapper = MappedColumnType.base[OrderType.Value, String](
    { os => os.toString }, { str => OrderType.withName(str) }
  )

  implicit val orderSideMapper = MappedColumnType.base[OrderSide.Value, String](
    { os => os.toString }, { str => OrderSide.withName(str) }
  )

  protected val orders = TableQuery[Orders]

  class Orders(tag: Tag) extends Table[Order](tag, "orders") {
    def id = column[UUID]("id", O.PrimaryKey)
    def userId = column[UUID]("user_id")
    def apiKeyId = column[UUID]("user_api_key_id")
    def exchangeName = column[Exchange.Value]("exchange_name")
    def exchangeOrderId = column[String]("exchange_order_id")
    def exchangeMarketName = column[String]("exchange_market_name")
    def marketName = column[String]("market_name")
    def side = column[OrderSide.Value]("side")
    def otype = column[OrderType.Value]("type")
    def price = column[BigDecimal]("price")
    def quantity = column[BigDecimal]("quantity")
    def quantityRemaining = column[BigDecimal]("quantity_remaining")
    def status = column[OrderStatus.Value]("status")
    def conditions = column[Json]("conditions")
    def createdOn = column[OffsetDateTime]("created_on")
    def updatedOn = column[OffsetDateTime]("updated_on")

    //def buyConditions = column[String]("buy_conditions")
    //def stopLossConditions = column[String]("stop_loss_conditions")
    //def takeProfitConditions = column[String]("profit_conditions")
    //def buyTime = column[OffsetDateTime]("bought_time")
    //def buyPrice = column[BigDecimal]("bought_price")
    //def buyCondition = column[String]("bought_condition")
    //def sellTime = column[OffsetDateTime]("sold_time")
    //def sellPrice = column[Double]("sold_price")
    //def sellCondition = column[String]("sold_condition")

    //def orderInfo = (
    //  exchangeName,
    //  marketName,
    //  currency,
    //  currencyLong,
    //  baseCurrencyAbbrev,
    //  baseCurrencyName) <>((MarketInfo.apply _).tupled, MarketInfo.unapply)

    //def tradeStatColumns = (
    //  buyTime.?,
    //  buyPrice.?,
    //  buyCondition.?,
    //  sellTime.?,
    //  sellPrice.?,
    //  sellCondition.?,
    //  currencyQuantity.?) <>((TradeStat.apply _).tupled, TradeStat.unapply)

    def * = (
      id,
      userId,
      apiKeyId,
      exchangeName,
      exchangeOrderId,
      exchangeMarketName,
      marketName,
      side,
      otype,
      price,
      quantity,
      quantityRemaining,
      status,
      conditions,
      createdOn,
      updatedOn
      ) <>
      ((Order.apply _).tupled, Order.unapply)
   }
}
