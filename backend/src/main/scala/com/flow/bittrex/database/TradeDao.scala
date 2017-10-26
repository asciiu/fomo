package com.flow.bittrex.database

import com.flow.bittrex.models.BittrexTrade
import scala.concurrent.Future

trait TradeDao {
  def insert(trade: BittrexTrade): Future[Int]
}
