package com.flow.marketmaker.services

import akka.actor.{Actor, ActorLogging, Props}
import com.flow.bittrex.database.postgres.SqlTradeDao
import com.flow.bittrex.models.BittrexTrade
import com.flow.marketmaker.models.MarketStructures.MarketUpdate
import com.flow.marketmaker.models.{BuyOrder, SimpleConditionalFactory, TradeOrder, TradeType}
import com.softwaremill.bootzooka.common.sql.SqlDatabase
import models.BasicUserData

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global


object MarketService {
  def props(marketName: String, sqlDatabase: SqlDatabase)(implicit context: ExecutionContext) =
    Props(new MarketService(marketName, sqlDatabase))

  case object ReturnAllData
  case object ReturnLatestMessage

  case class CreateOrder(forUser: BasicUserData, buyOrder: BuyOrder)
}


class MarketService(val marketName: String, sqlDatabase: SqlDatabase) extends Actor
  with ActorLogging {

  import MarketService._
  lazy val tradeDao = new SqlTradeDao(sqlDatabase)

  // TODO need a better collection here
  val orders = collection.mutable.ListBuffer[TradeOrder]()


  override def preStart() = {
    //eventBus.subscribe(self, PoloniexEventBus.BTCPrice)
  }


  override def postStop() = {
    //eventBus.unsubscribe(self, PoloniexEventBus.BTCPrice)
    //log.info(s"Shutdown $marketName service")
  }


  def receive: Receive = {
    case update: MarketUpdate =>
      updateState(update)

    case CreateOrder(user, buyOrder) =>
      createOrder(user, buyOrder)
  }


  private def updateState(update: MarketUpdate) = {
    val lastPrice = update.Last

    val executeOrders = orders.filter(_.evaluate(lastPrice))

    executeOrders.foreach{ to =>
      println(s"Last Price: ${lastPrice}")
      println(s"Execute ${to.side} ${to.quantity} ${to.currencyName} for user: ${to.userId}")

      // TODO save to the dao a record of this trades close
      //
    }

    // remove the executed orders
    orders --= executeOrders
  }


  private def createOrder(user: BasicUserData, newOrder: BuyOrder) = {

    val conditions = newOrder.buyConditions.map{ c => SimpleConditionalFactory.makeCondition(c.operator, c.value) }

    val order = TradeOrder(
      userId = user.id,
      exchangeName = newOrder.exchangeName,
      marketName = newOrder.marketName,
      currencyName = newOrder.marketName.split("-")(1),
      side = TradeType.Buy,
      quantity = newOrder.quantity,
      orConditions = conditions
    )

    orders.append(order)


    // TODO this needs to be merged with the TradeOrder concept
    tradeDao.insert(BittrexTrade.withRandomUUID(order.marketName, true, order.quantity, 0.0))
  }
}

