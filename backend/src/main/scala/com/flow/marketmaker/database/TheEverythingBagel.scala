package com.flow.marketmaker.database

import com.flow.marketmaker.models.{Order, OrderStatus, Trade}
import java.util.UUID
import scala.concurrent.Future

trait TheEverythingBagelDao {

  // orders
  def insert(order: Order): Future[Order]
  def update(order: Order): Future[Option[Order]]
  def findByOrderId(id: UUID): Future[Option[Order]]
  def findAllByUserId(userId: UUID): Future[Seq[Order]]
  def findAllByOrderStatus(marketName: String, status: OrderStatus.Value): Future[Seq[Order]]

  // trades
  def insert(trade: Trade): Future[Int]
}
