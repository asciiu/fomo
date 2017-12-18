package com.flowy.common.database.postgres.schema

import java.util.UUID
import com.flowy.common.models.{Exchange, Market}
import com.flowy.common.slick.MyPostgresDriver.api._


trait SqlMarket extends SqlSchema {

  protected val markets = TableQuery[Markets]

  class Markets(tag: Tag) extends Table[Market](tag, "markets") {
    def id = column[UUID]("id", O.PrimaryKey)
    def exchangeName = column[Exchange.Value]("exchange_name")
    def marketName = column[String]("market_name")
    def currencyName = column[String]("market_currency")
    def currencyNameLong = column[String]("market_currency_long")
    def baseCurrency = column[String]("base_currency")
    def baseCurrencyLong = column[String]("base_currency_long")


    def * = (id,
      exchangeName,
      marketName,
      currencyName,
      currencyNameLong,
      baseCurrency,
      baseCurrencyLong) <>
      ((Market.apply _).tupled, Market.unapply)
   }
}
