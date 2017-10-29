package com.flow.marketmaker.database

import java.util.UUID

import com.flow.bittrex.models.BittrexTrade
import com.flow.marketmaker.models.Order

import scala.concurrent.Future

trait TheEverythingBagelDao {

  // orders
  def insert(order: Order): Future[Order]
  def update(order: Order): Future[Order]
  def findAllByUserId(userId: UUID): Future[Seq[Order]]
}
