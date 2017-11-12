package com.flowy.marketmaker.database

import com.flowy.marketmaker.models.{Trade, TradeStatus}
import java.util.UUID

import scala.concurrent.Future

trait TheEverythingBagelDao {

  // trades
  def insert(trade: Trade): Future[Int]

  def findTradesByUserId(userId: UUID, marketName: Option[String], exchangeName: Option[String], statuses: List[String]): Future[Seq[Trade]]

  def findTradesByStatus(marketName: String, tradeStatus: TradeStatus.Value): Future[Seq[Trade]]

  def findTradeById(tradeId: UUID): Future[Option[Trade]]

  def updateTrade(trade: Trade): Future[Option[Trade]]
}