package com.flowy.common.database.postgres.schema

import java.time.OffsetDateTime
import java.util.UUID

import com.flowy.common.models._
import com.flowy.common.utils.sql.SqlDatabase


trait SqlUserbalance {
  protected val database: SqlDatabase

  import com.flowy.common.slick.MyPostgresDriver.api._
  import database._

  protected val balances = TableQuery[UserBalances]

  class UserBalances(tag: Tag) extends Table[Balance](tag, "user_balances") {
    def id = column[UUID]("id", O.PrimaryKey)
    def userId = column[UUID]("user_id")
    def exchangeName = column[String]("exchange_name")
    def currencyName = column[String]("currency_name")
    def currencyNameLong = column[String]("currency_name_long")
    def address = column[String]("blockchain_address")
    def available = column[Double]("available")
    def exchangeTotal = column[Double]("exchange_total")
    def exchangeAvailable = column[Double]("exchange_available")
    def pending = column[Double]("pending_deposit")

    def * = (id,
      userId,
      exchangeName,
      currencyName,
      currencyNameLong,
      address.?,
      available,
      exchangeTotal,
      exchangeAvailable,
      pending.?) <>
      ((Balance.apply _).tupled, Balance.unapply)
   }
}
