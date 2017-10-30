package com.flow.marketmaker.database.postgres

import java.sql.JDBCType
import java.util.UUID

import com.flow.marketmaker.database.TheEverythingBagelDao
import com.flow.marketmaker.database.postgres.schema.SqlOrder
import com.flow.marketmaker.models.{Order, OrderStatus}
import com.softwaremill.bootzooka.common.sql.SqlDatabase

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.{PositionedParameters, SetParameter}


class SqlTheEverythingBagelDao(protected val database: SqlDatabase)(implicit val ec: ExecutionContext)
  extends TheEverythingBagelDao with SqlOrder {

  import database._
  import database.driver.api._

  implicit object SetUUID extends SetParameter[UUID] { def apply(v: UUID, pp: PositionedParameters) { pp.setObject(v, JDBCType.BINARY.getVendorTypeNumber) } }

  def insert(order: Order): Future[Order] = {
    val query = orders returning orders.map(_.id) into ((o, id) => o.copy(id = Some(id)))
    db.run(query += order)
  }

  /**
    * Update an order
    * @param order
    * @return Some(order) or None
    */
  def update(order: Order): Future[Option[Order]] = {
    val updatedOrder = (orders returning orders).insertOrUpdate(order)
    db.run(updatedOrder)
  }

  def findAllByUserId(userId: UUID): Future[Seq[Order]] = {
    db.run(orders.filter(_.userId === userId).result)
  }

  def findByOrderId(id: Long): Future[Option[Order]] = {
    db.run(orders.filter(_.id === id).result.headOption)
  }

  def findAllByOrderStatus(marketName: String, status: OrderStatus.Value): Future[Seq[Order]] = {
    db.run(orders.filter(o => o.status === status && o.marketName === marketName).result)
  }
}
