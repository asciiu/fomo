package com.flowy.common.database

import com.flowy.common.models.{Trade, TradeStatus, UserDevice}
import java.util.UUID

import scala.concurrent.Future


trait TheEverythingBagelDao {

  def userKeyDao: UserKeyDao

  // manage trades
  def insert(trade: Trade): Future[Int]

  def findTradesByUserId(userId: UUID, marketName: Option[String], exchangeName: Option[String], statuses: List[String]): Future[Seq[Trade]]

  def findTradesByStatus(marketName: String, tradeStatus: TradeStatus.Value): Future[Seq[Trade]]

  def findTradeById(tradeId: UUID): Future[Option[Trade]]

  def updateTrade(trade: Trade): Future[Option[Trade]]

  def deleteTrade(trade: Trade): Future[Option[Trade]]

  // manage push tokens
  def insert(userDevice: UserDevice): Future[Int]
}
