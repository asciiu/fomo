package com.flowy.common.database.postgres.schema

import java.util.UUID
import com.flowy.common.models.{Exchange, Market}
import com.flowy.common.slick.MyPostgresDriver.api._


trait SqlMarket extends SqlSchema {

  protected val markets = TableQuery[Markets]
  import database._

  class Markets(tag: Tag) extends Table[Market](tag, "markets") {
    def id = column[UUID]("id", O.PrimaryKey)
    def exchangeName = column[Exchange.Value]("exchange_name")
    def marketName = column[String]("market_name")
    def currency = column[String]("currency")
    def currencyLong = column[String]("currency_long")
    def baseCurrency = column[String]("base_currency")
    def baseCurrencyLong = column[String]("base_currency_long")


    def * = (id,
      exchangeName,
      marketName,
      currency,
      currencyLong,
      baseCurrency,
      baseCurrencyLong) <>
      ((Market.apply _).tupled, Market.unapply)
   }
}
