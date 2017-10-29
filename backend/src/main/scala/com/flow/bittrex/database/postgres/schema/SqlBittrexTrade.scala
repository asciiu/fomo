package com.flow.bittrex.database.postgres.schema

import java.time.OffsetDateTime
import java.util.UUID

import com.flow.marketmaker.models.{Order, OrderStatus, OrderType, Conditional}
import com.softwaremill.bootzooka.common.sql.SqlDatabase

//trait SqlOrder {
//  protected val database: SqlDatabase
//
//  import database._
//  import database.driver.api._
//
//  protected val orders = TableQuery[Orders]
//
//  protected class Orders(tag: Tag) extends Table[Order](tag, "orders") {
//
//    def id                 = column[Long]("id", O.PrimaryKey, O.AutoInc)
//    def userId             = column[UUID]("user_id")
//    def exchangeName       = column[String]("exchange_name")
//    def marketName         = column[String]("market_name")
//    def createdTime        = column[OffsetDateTime]("created_time")
//    def completedTime      = column[Option[OffsetDateTime]]("completed_time")
//    def completedCondition = column[Option[String]]("completed_condition")
//    def priceActual        = column[Option[Double]]("price_actual")
//    def quantity           = column[Double]("quantity")
//    def orderType          = column[String]("order_type")
//    def status             = column[String]("status")
//    def conditions         = column[Array[java.sql.Blob]]("conditions")
//
//    def * = (
//      id,
//      userId,
//      exchangeName,
//      marketName,
//      createdTime,
//      completedTime.?,
//      completedCondition.?,
//      priceActual.?,
//      quantity,
//      orderType,
//      status,
//      conditions) <>
//      ((Order.apply _).tupled, Order.unapply)
//  }
//
//}

//trait SqlTrade {
//  protected val database: SqlDatabase
//
//  import database._
//  import database.driver.api._
//
//  protected val bittrexTrades = TableQuery[BittrexTrades]
//
//  protected class BittrexTrades(tag: Tag) extends Table[BittrexTrade](tag, "bittrex_trades") {
//
//    // format: OFF
//    def id              = column[UUID]("id", O.PrimaryKey, O.AutoInc)
//    def marketName      = column[String]("market_name")
//    def isOpen          = column[Boolean]("is_open")
//    def quantity        = column[Double]("quantity")
//    def bidPrice        = column[Double]("bid_price")
//    def createdOn       = column[OffsetDateTime]("created_on")
//    def purchasedPrice  = column[Option[Double]]("purchased_price")
//    def purchasedOn     = column[Option[OffsetDateTime]]("purchased_on")
//
//    def * = (
//      id,
//      marketName,
//      isOpen,
//      quantity,
//      bidPrice,
//      createdOn,
//      purchasedPrice,
//      purchasedOn) <>
//      ((BittrexTrade.apply _).tupled, BittrexTrade.unapply)
//    // format: ON
//  }
//}
