package com.flow.marketmaker.database.postgres

import java.sql.JDBCType
import java.time.OffsetDateTime
import java.util.UUID

import com.flow.marketmaker.database.TheEverythingBagelDao
import com.flow.marketmaker.database.postgres.schema.SqlOrder
import com.flow.marketmaker.models.Order
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
//  def insert(order: Order): Future[Order] = {
//    val json = order.orConditions.map(_.toJson()).mkString("[", ",", "]")
//
//    val queryId = sql"""select nextval('orders_id_seq')""".as[Int]
//
//    def insertQuery(id: Int) = sqlu"""insert into orders(id, user_id, exchange_name, market_name, created_time, quantity, order_type, status, conditions)
//                    values ($id, ${order.userId}, ${order.exchangeName}, ${order.marketName},${order.createdTime.toString}::timestamp,
//      ${order.quantity}, ${order.orderType.toString}, ${order.status.toString}, $json::jsonb)"""
//
//    val actions = for {
//      newOrderId <- queryId
//      _ <- insertQuery(newOrderId(0))
//    } yield newOrderId
//
//    // id: Vector[Int]
//    db.run(actions.transactionally).map{ id =>
//      // prints the number of affected rows
//      // which amount to 1 if the insert was successful
//      order.copy(id = Some(id(0)))
//    }
//  }


//  def insert1(order: Order): Future[Order] = {
//    //val json = order.orConditions.map(_.toJson()).mkString("[", ",", "]")
//
//    //val queryId = sql"""select nextval('orders_id_seq')""".as[Int]
//
//    //def insertQuery(id: Int) = sqlu"""insert into orders(id, user_id, exchange_name, market_name, created_time, quantity, order_type, status, conditions)
//    //                values ($id, ${order.userId}, ${order.exchangeName}, ${order.marketName},${order.createdTime.toString}::timestamp,
//    //  ${order.quantity}, ${order.orderType.toString}, ${order.status.toString}, $json::jsonb)"""
//
//    //val actions = for {
//      newOrderId <- queryId
//      _ <- insertQuery(newOrderId(0))
//    } yield newOrderId
//
//    // id: Vector[Int]
//    db.run(actions.transactionally).map{ id =>
//      // prints the number of affected rows
      // which amount to 1 if the insert was successful
//      order.copy(id = Some(id(0)))
//    }
//  }

  /**
    * You can currently only upadate the completed time,
    * @param order
    * @return
    */
  def update(order: Order): Future[Order] = {

    order.id match {
      case Some(id) =>

        val completedTime = order.completedTime match {
          case Some(t) => s""", completed_time='$t'::timestamp"""
          case None => ""
        }

        val priceActual = order.priceActual match {
          case Some(p) => s", price_actual=$p"
          case None => ""
        }

        val completedCondition = order.completedCondition match {
          case Some(c) => s""", completed_condition='$c'"""
          case None => ""
        }

        val updateQuery = sqlu"""update orders set status='#${order.status.toString}'#$completedTime#$completedCondition#${priceActual} where id = $id"""

        db.run(updateQuery).map{ res => order}.recover {
          case e:  java.sql.SQLException =>
            println("Caught exception: " + e.getMessage)
            order
        }

      case None =>
        Future.successful(order)
    }
  }

  def findAllByUserId(userId: UUID): Future[Seq[Order]] = {
    db.run(orders.filter(_.userId === userId).result)
  }
}
