package com.flow.marketmaker.services

import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
import com.flow.marketmaker.database.MarketUpdateDao
import com.flow.marketmaker.models.MarketStructures.{MarketMessage, MarketUpdate}
import com.flow.marketmaker.models.TradeOrder

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

object MarketService {
  def props(marketName: String)(implicit context: ExecutionContext) =
    Props(new MarketService(marketName))

  case object ReturnAllData
  case object ReturnLatestMessage

  case class PlaceOrder(tradeOrder: TradeOrder)
}

class MarketService(val marketName: String) extends Actor
  with ActorLogging {

  import MarketService._

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
      val lastPrice = update.Last

      val executeOrders = orders.filter(_.evaluate(lastPrice))

      executeOrders.foreach{ to =>
        println(s"Last Price: ${lastPrice}")
        println(s"Execute ${to.side} ${to.quantity} ${to.currencyName} for user: ${to.userId}")
      }

      // remove the executed orders
      orders --= executeOrders

    case PlaceOrder(tradeOrder) =>
      orders.append(tradeOrder)
  }
}

