package com.flowy.common.database.postgres

import java.sql.JDBCType
import java.util.UUID

import com.flowy.common.utils.sql.SqlDatabase
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.database.postgres.schema._
import com.flowy.common.models.User.UserId
import com.flowy.common.models._

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.{PositionedParameters, SetParameter}

import scala.util.{Failure, Success}


class SqlTheEverythingBagelDao(protected val database: SqlDatabase)(implicit val ec: ExecutionContext)
  extends TheEverythingBagelDao
    with SqlTrade
    with SqlUserDeviceSchema
    with SqlUserbalance
    with SqlMarket
    with SqlTradeHistory
    with SqlOrder
    with SqlOrderFill
    with SqlUserSchema {

  import database._
  import database.driver.api._

  lazy val userKeyDao = new SqlUserKeyDao(database)

  implicit object SetUUID extends SetParameter[UUID] { def apply(v: UUID, pp: PositionedParameters) { pp.setObject(v, JDBCType.BINARY.getVendorTypeNumber) } }


  /*******************************************************************************************
    * Balance Stuff
    *****************************************************************************************/

  /**
    * Insert balances
    * @param userBalances
    * @return
    */
  def insert(userBalances: Seq[Balance]): Future[Int] = {
    db.run(balances ++= userBalances).map {
      case Some(count) => count
      case None => 0
    }
  }

  def findBalancesByUserId(userId: UUID): Future[Seq[Balance]] = {
    db.run(balances.filter(b => b.userId === userId).result)
  }

  def findBalancesByUserIdAndEx(userId: UUID, exchange: Exchange.Value): Future[Seq[Balance]] = {
    db.run(balances.filter(b => b.userId === userId && b.exchangeName === exchange).result)
  }

  def findBalance(userId: UUID, apiKey: UUID, currency: String): Future[Option[Balance]] = {
    db.run(balances.filter(b => b.userId === userId && b.apiKeyId === apiKey && b.currency === currency).result.headOption)
  }

  def updateBalance(balance: Balance): Future[Option[Balance]] = {
    val updatedBal = (balances returning balances).insertOrUpdate(balance)

    // returns None if updated
    // http://slick.lightbend.com/doc/3.2.0/queries.html#updating
    db.run(updatedBal).map {
      case None => Some(balance)
      case _ => None
    }
  }

  /*******************************************************************************************
    * Order Stuff
    *****************************************************************************************/
  def insert(order: Order): Future[Int] = {
    db.run(orders += order).andThen {
        case Success(count) => count
        case Failure(e) => 0
    }
  }
  def findOrderBy(orderId: UUID): Future[Option[Order]] = {
    db.run(orders.filter(o => o.id === orderId).result.headOption)
  }

  def insert(of: OrderFill): Future[Int] = {
    db.run(orderFills += of).andThen {
      case Success(count) => count
      case Failure(e) => 0
    }
  }

  def findOrderFillBy(orderFillId: UUID): Future[Option[OrderFill]] = {
    db.run(orderFills.filter(of => of.id === orderFillId).result.headOption)
  }

  /*******************************************************************************************
    * Trade Stuff
    *****************************************************************************************/

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
    db.run(trades += trade).andThen {
      case Success(x) => x
      case Failure(e) => 0
    }
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

  def findTradesByStatus(marketName: String, tradeStatus: Seq[TradeStatus.Value]): Future[Seq[Trade]] = {
    db.run(trades.filter(t => t.marketName === marketName && t.status.inSetBind(tradeStatus) ).result)
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

  def deleteDevice(userDevice: UserDevice): Future[Option[UserDevice]] = {
    val action = userDevices.filter(_.id === userDevice.id).delete

    db.run(action.asTry).map { result =>
      result match {
        case Success(count) if count > 0 => Some(userDevice)
        case _ => None
      }
    }
  }

  def findUserDevice(userId: UUID, deviceId: String): Future[Option[UserDevice]] = {
    db.run(userDevices.filter(d => d.userId === userId && d.deviceId === deviceId).result.headOption)
  }

  def findUserDevice(userId: UUID, deviceId: UUID): Future[Option[UserDevice]] = {
    db.run(userDevices.filter(d => d.userId === userId && d.id === deviceId).result.headOption)
  }

  def findUserDevices(userId: UUID): Future[Seq[UserDevice]] = {
    db.run(userDevices.filter(d => d.userId === userId).result)
  }

  /** insert a users device token
    * @param userDevice
    * @return
    */
  def insert(userDevice: UserDevice): Future[Int] = {
    db.run(userDevices += userDevice)
  }

  def updateDevice(userDevice: UserDevice): Future[Option[UserDevice]] = {
    val updateDevice = (userDevices returning userDevices).insertOrUpdate(userDevice)

    // returns None if updated
    // http://slick.lightbend.com/doc/3.2.0/queries.html#updating
    db.run(updateDevice).map {
      case None => Some(userDevice)
      case _ => None
    }
  }

  /*******************************************************************************************
    * Market management below here
    *****************************************************************************************/
  def insert(market: Market): Future[Int] = {
    db.run(markets += market)
  }
  def findAllMarkets(exchange: Exchange.Value): Future[Seq[Market]] = {
    db.run(markets.filter(_.exchangeName === exchange).result)
  }
  def findMarketByName(exchange: Exchange.Value, marketName: String): Future[Option[Market]] = {
    db.run(markets.filter(m => m.exchangeName === exchange && m.marketName === marketName).result.headOption)
  }

  /*******************************************************************************************
    * TradeHistory
    *****************************************************************************************/
  def insert(event: TradeHistory): Future[Int] = {
    db.run(tradeHistory += event)
  }
  def findTradeHistoryByUserId(userId: UUID): Future[Seq[TradeHistory]] = {
    db.run(tradeHistory.filter(_.userId === userId).result)
  }
  def findTradeHistoryByUserId(userId: UUID, exchangeNameOpt: Option[String], marketNameOpt: Option[String]): Future[Seq[TradeHistory]] = {
    val query = for {
      tradeRecords <- tradeHistory.filter(r =>
        r.userId === userId &&
          exchangeNameOpt.map(m => r.exchangeName.toLowerCase === m.toLowerCase).getOrElse(slick.lifted.LiteralColumn(true)) &&
          marketNameOpt.map(n => r.marketName.toLowerCase === n.toLowerCase).getOrElse(slick.lifted.LiteralColumn(true))
      )
    } yield tradeRecords

    db.run(query.result)
  }


  /*******************************************************************************************
    * User
    *****************************************************************************************/
  private def findOneWhere(condition: Users => Rep[Boolean]) = db.run(users.filter(condition).result.headOption)

  def add(user: User): Future[Int] = db.run(users += user)

  def findById(userId: UserId): Future[Option[User]] = findOneWhere(_.id === userId)

  def findBasicDataById(userId: UserId): Future[Option[UserData]] =
    db.run(users.filter(_.id === userId).result.headOption).map{
      case Some(user) => Some(UserData.fromUser(user))
      case None => None
    }

  def findByEmail(email: String): Future[Option[User]] = findOneWhere(_.email.toLowerCase === email.toLowerCase)

  def changePassword(userId: UserId, newPassword: String): Future[Int] =
    db.run(users.filter(_.id === userId).map(_.password).update(newPassword))

  def changeEmail(userId: UserId, newEmail: String): Future[Int] =
    db.run(users.filter(_.id === userId).map(_.email).update(newEmail))
}
