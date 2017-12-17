package com.flowy.common.database.postgres.schema

import com.flowy.common.models._
import com.flowy.common.utils.sql.SqlDatabase
import java.util.UUID


trait SqlUserbalance {
  protected val database: SqlDatabase

  import com.flowy.common.slick.MyPostgresDriver.api._
  import database._

  implicit val exMapper = MappedColumnType.base[Exchange.Value, String](
    { os => os.toString }, { str => Exchange.withName(str) }
  )

  protected val balances = TableQuery[UserBalances]

  class UserBalances(tag: Tag) extends Table[Balance](tag, "user_balances") {
    def id = column[UUID]("id", O.PrimaryKey)
    def userId = column[UUID]("user_id")
    def apiKeyId = column[UUID]("user_api_key_id")
    def exchangeName = column[Exchange.Value]("exchange_name")
    def currencyName = column[String]("currency_name")
    def currencyNameLong = column[String]("currency_name_long")
    def address = column[String]("blockchain_address")
    def available = column[Double]("available")
    def exchangeTotal = column[Double]("exchange_total")
    def exchangeAvailable = column[Double]("exchange_available")
    def pending = column[Double]("pending_deposit")

    def * = (id,
      userId,
      apiKeyId,
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
