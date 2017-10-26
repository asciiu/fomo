package com.flow.bittrex.database.postgres.schema

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

import com.flow.bittrex.models.BittrexTrade
import com.softwaremill.bootzooka.common.sql.SqlDatabase


trait SqlBittrexTrade {
  protected val database: SqlDatabase

  import database._
  import database.driver.api._

  protected val bittrexTrades = TableQuery[BittrexTrades]

  protected class BittrexTrades(tag: Tag) extends Table[BittrexTrade](tag, "bittrex_trades") {

    // format: OFF
    def id              = column[UUID]("id", O.PrimaryKey, O.AutoInc)
    def marketName      = column[String]("market_name")
    def isOpen          = column[Boolean]("is_open")
    def quantity        = column[Double]("quantity")
    def bidPrice        = column[Double]("bid_price")
    def createdOn       = column[OffsetDateTime]("created_on")
    def purchasedPrice  = column[Option[Double]]("purchased_price")
    def purchasedOn     = column[Option[OffsetDateTime]]("purchased_on")

    def * = (
      id,
      marketName,
      isOpen,
      quantity,
      bidPrice,
      createdOn,
      purchasedPrice,
      purchasedOn) <>
      ((BittrexTrade.apply _).tupled, BittrexTrade.unapply)
    // format: ON
  }
}
