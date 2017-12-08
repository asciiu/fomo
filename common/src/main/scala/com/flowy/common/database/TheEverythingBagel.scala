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
  def deleteDevice(device: UserDevice): Future[Option[UserDevice]]
  def findUserDevice(userId: UUID, deviceId: UUID): Future[Option[UserDevice]]
  def findUserDevice(userId: UUID, deviceId: String): Future[Option[UserDevice]]
  def findUserDevices(userId: UUID): Future[Seq[UserDevice]]
  def insert(userDevice: UserDevice): Future[Int]
  def updateDevice(userDevice: UserDevice): Future[Option[UserDevice]]
}
