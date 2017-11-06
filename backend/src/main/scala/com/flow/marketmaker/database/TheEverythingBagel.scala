package com.flow.marketmaker.database

import com.flow.marketmaker.models.Trade
import java.util.UUID
import scala.concurrent.Future

trait TheEverythingBagelDao {

  // trades
  def insert(trade: Trade): Future[Int]
  def findTradesByUserId(userId: UUID): Future[Seq[Trade]]
  def findTradeById(tradeId: UUID): Future[Option[Trade]]
}
