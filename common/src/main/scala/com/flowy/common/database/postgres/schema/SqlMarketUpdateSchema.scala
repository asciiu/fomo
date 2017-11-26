package com.flowy.common.database.postgres.schema

import java.time.OffsetDateTime
import com.flowy.common.models.MarketStructures.MarketUpdate
import com.flowy.common.utils.sql.SqlDatabase


/**
  * The schemas are in separate traits, so that if your DAO would require to access (e.g. join) multiple tables,
  * you can just mix in the necessary traits and have the `TableQuery` definitions available.
  */
trait SqlMarketUpdateSchema {

  protected val database: SqlDatabase

  import database._
  import database.driver.api._

  protected val marketUpdates = TableQuery[MarketUpdates]

  protected class MarketUpdates(tag: Tag) extends Table[MarketUpdate](tag, "bittrex_market_updates") {

    // format: OFF
    def id              = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def marketName      = column[String]("market_name")
    def high            = column[Double]("high")
    def low             = column[Double]("low")
    def volume          = column[Double]("volume")
    def last            = column[Double]("last")
    def baseVolume      = column[Double]("base_volume")
    def timestamp       = column[OffsetDateTime]("timestamp")
    def bid             = column[Double]("bid")
    def ask             = column[Double]("ask")
    def openBuyOrders   = column[Int]("open_buy_orders")
    def openSellOrders  = column[Int]("open_sell_orders")
    def prevDay         = column[Double]("prev_day")
    def createdOn       = column[OffsetDateTime]("created_on")

    def * = (
      id.?,
      marketName,
      high,
      low,
      volume,
      last,
      baseVolume,
      timestamp,
      bid,
      ask,
      openBuyOrders,
      openSellOrders,
      prevDay,
      createdOn) <>
      ((MarketUpdate.apply _).tupled, MarketUpdate.unapply)
    // format: ON
  }
}

