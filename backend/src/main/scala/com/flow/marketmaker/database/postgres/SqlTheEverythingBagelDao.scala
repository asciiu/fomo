package com.flow.marketmaker.database.postgres

import java.sql.JDBCType
import java.util.UUID

import com.flow.marketmaker.database.TheEverythingBagelDao
import com.flow.marketmaker.database.postgres.schema.SqlTrade
import com.flow.marketmaker.models.Trade
import com.softwaremill.bootzooka.common.sql.SqlDatabase
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.{PositionedParameters, SetParameter}


class SqlTheEverythingBagelDao(protected val database: SqlDatabase)(implicit val ec: ExecutionContext)
  extends TheEverythingBagelDao with SqlTrade {

  import database._
  import database.driver.api._

  implicit object SetUUID extends SetParameter[UUID] { def apply(v: UUID, pp: PositionedParameters) { pp.setObject(v, JDBCType.BINARY.getVendorTypeNumber) } }

  /**
    * Insert trade
    */
  def insert(trade: Trade): Future[Int] = {
    db.run(trades += trade)
  }

  def findTradesByUserId(userId: UUID): Future[Seq[Trade]] = {
    db.run(trades.filter(_.userId === userId).result)
  }

  def findTradeById(tradeId: UUID): Future[Option[Trade]] = {
    db.run(trades.filter(_.id === tradeId).result.headOption)
  }
}
