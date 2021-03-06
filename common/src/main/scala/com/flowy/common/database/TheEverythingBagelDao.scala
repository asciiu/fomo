package com.flowy.common.database

import com.flowy.common.models._
import com.flowy.common.models.User.UserId
import java.util.UUID
import scala.concurrent.Future

trait TheEverythingBagelDao {

  def userKeyDao: UserKeyDao

  // manage balances
  def insert(balances: Seq[Balance]): Future[Int]
  def findBalancesByUserId(userId: UUID): Future[Seq[Balance]]
  def findBalancesByUserIdAndEx(userId: UUID, exchange: Exchange.Value): Future[Seq[Balance]]
  def findBalance(userId: UUID, apiKey: UUID, currency: String): Future[Option[Balance]]
  def updateBalance(balance: Balance): Future[Option[Balance]]

  // orders
  def insert(order: Order): Future[Int]
  def findOrderBy(id: UUID): Future[Option[Order]]

  // order fills
  def insert(orderfill: OrderFill): Future[Int]
  def findOrderFillBy(id: UUID): Future[Option[OrderFill]]

  // manage market info
  def insert(market: Market): Future[Int]
  def findAllMarkets(exchange: Exchange.Value): Future[Seq[Market]]
  def findMarketByName(exchange: Exchange.Value, marketName: String): Future[Option[Market]]

  // manage trades
  def insert(trade: Trade): Future[Int]
  def findTradesByUserId(userId: UUID, marketName: Option[String], exchangeName: Option[String], statuses: List[String]): Future[Seq[Trade]]
  def findTradesByStatus(marketName: String, tradeStatus: TradeStatus.Value): Future[Seq[Trade]]
  def findTradesByStatus(marketName: String, tradeStatus: Seq[TradeStatus.Value]): Future[Seq[Trade]]
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

  // insert trade history
  def insert(event: TradeHistory): Future[Int]
  def findTradeHistoryByUserId(userId: UUID): Future[Seq[TradeHistory]]
  def findTradeHistoryByUserId(userId: UUID, exchangeNameOpt: Option[String], marketNameOpt: Option[String]): Future[Seq[TradeHistory]]

  // users
  def add(user: User): Future[Int]
  def findById(userId: UserId): Future[Option[User]]
  def findBasicDataById(userId: UserId): Future[Option[UserData]]
  def findByEmail(email: String): Future[Option[User]]
  def changePassword(userId: UserId, newPassword: String): Future[Int]
  def changeEmail(userId: UserId, newEmail: String): Future[Int]
}
