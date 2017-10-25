package com.flow.marketmaker.services

import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
import com.flow.marketmaker.database.MarketUpdateDao
import com.flow.marketmaker.models.MarketStructures.{MarketMessage, MarketUpdate}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

object MarketService {
  def props(marketName: String)(implicit context: ExecutionContext) =
    Props(new MarketService(marketName))

  case object ReturnAllData
  case object ReturnLatestMessage

  //
  /*
  id: "1",
  exchange: "bittrex",
  exchangeName: "bittrex",
  marketName: "BAT-BTC",
  marketCurrency: "BAT",
  marketCurrencyLong: "Basic Attention Token",
  baseCurrency: "BTC",
  baseCurrencyLong: "Bitcoin",
  createdTime: "2014-07-12T03:41:25.323",
  boughtTime:"2014-07-12T04:42:25.323",
  quantity: 1000,
  boughtPriceAsked: 0.000045,
  boughtPriceActual: 0.000044,
  //soldTime: "",
  //soldPriceAsked: 0.00005,
  //soldPriceActual: 0.000051,
  status: "bought",
  buyConditions: [{id: 1, type: "simpleConditional", indicator: "price", operator: "<=", value: 0.000045}],
  sellConditions: [{id: 1, type: "simpleConditional", indicator: "price", operator: ">=", value: 0.00005}]
  */
  case class Order(exchange: String, marketName: String, userId: UUID, price: Double)
}

class MarketService(val marketName: String) extends Actor
  with ActorLogging {

  import MarketService._

  // TODO need a better collection here
  val orders = collection.mutable.ListBuffer[Order]()

  override def preStart() = {
    //eventBus.subscribe(self, PoloniexEventBus.BTCPrice)
  }
  override def postStop() = {
    //eventBus.unsubscribe(self, PoloniexEventBus.BTCPrice)
    //log.info(s"Shutdown $marketName service")
  }

  def receive: Receive = {
    case update: MarketUpdate =>

      orders.foreach{ o =>
        if (o.price <= update.Last) {
          println("execute the order")
          // fill the order
          // remove the order from the orders collection
        }
      }

    case order: Order =>
      orders.append(order)
  }
}

