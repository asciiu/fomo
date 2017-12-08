package com.flowy.common.database.postgres

import java.sql.JDBCType
import java.util.UUID

import com.flowy.common.utils.sql.SqlDatabase
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.database.postgres.schema.{SqlTrade, SqlUserDeviceSchema}
import com.flowy.common.models.{Trade, TradeStatus, UserDevice}

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.{PositionedParameters, SetParameter}

import scala.util.Success


class SqlTheEverythingBagelDao(protected val database: SqlDatabase)(implicit val ec: ExecutionContext)
  extends TheEverythingBagelDao with SqlTrade with SqlUserDeviceSchema {

  import database._
  import database.driver.api._

  lazy val userKeyDao = new SqlUserKeyDao(database)

  implicit object SetUUID extends SetParameter[UUID] { def apply(v: UUID, pp: PositionedParameters) { pp.setObject(v, JDBCType.BINARY.getVendorTypeNumber) } }


  def deleteTrade(trade: Trade): Future[Option[Trade]] = {
    val action = trades.filter(_.id === trade.id).delete

    db.run(action.asTry).map { result =>
      result match {
        case Success(count) if count > 0 => Some(trade)
        case _ => None
      }
    }
  }

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
    db.run(trades.filter(t => t.id === tradeId).result.headOption)
  }

  def updateTrade(trade: Trade): Future[Option[Trade]] = {
    val updatedTrade = (trades returning trades).insertOrUpdate(trade)

    // returns None if updated
    // http://slick.lightbend.com/doc/3.2.0/queries.html#updating
    db.run(updatedTrade).map {
      case None => Some(trade)
      case _ => None
    }
  }

  /*******************************************************************************************
    * User Device Stuff below here
    *****************************************************************************************/

  /** insert a users device token
    * @param userDevice
    * @return
    */
  def insert(userDevice: UserDevice): Future[Int] = {
    db.run(userDevices += userDevice)
  }
}
