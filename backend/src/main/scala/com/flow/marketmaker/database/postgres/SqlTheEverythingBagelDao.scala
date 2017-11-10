package com.flow.marketmaker.database.postgres

import java.sql.JDBCType
import java.util.UUID

import com.flow.marketmaker.database.TheEverythingBagelDao
import com.flow.marketmaker.database.postgres.schema.SqlTrade
import com.flow.marketmaker.models.{Trade, TradeStatus}
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

  def findTradesByUserId(userId: UUID,
                         marketNameOpt: Option[String],
                         exchangeNameOpt: Option[String],
                         statusList: List[String]): Future[Seq[Trade]] = {

    val query = for {
      tradeRecords <- trades.filter(r =>
        r.userId === userId &&
          marketNameOpt.map(m => r.marketName.toLowerCase === m.toLowerCase).getOrElse(slick.lifted.LiteralColumn(true)) &&
          exchangeNameOpt.map(n => r.exchangeName.toLowerCase === n.toLowerCase).getOrElse(slick.lifted.LiteralColumn(true))
      )
    } yield tradeRecords

    if (statusList.nonEmpty) {
      db.run(query.result).map( _.filter(r => statusList.contains(r.status.toString)) )
    } else {
      db.run(query.result)
    }
  }

  def findTradesByStatus(marketName: String, tradeStatus: TradeStatus.Value): Future[Seq[Trade]] = {
    db.run(trades.filter(t => t.status === tradeStatus && t.marketName === marketName).result)
  }

  def findTradeById(tradeId: UUID): Future[Option[Trade]] = {
    db.run(trades.filter(_.id === tradeId).result.headOption)
  }

  def updateTrade(trade: Trade): Future[Option[Trade]] = {
    val updatedTrade = (trades returning trades).insertOrUpdate(trade)
    db.run(updatedTrade)
  }
}
