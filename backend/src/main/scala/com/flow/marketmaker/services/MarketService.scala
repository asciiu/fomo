package com.flow.marketmaker.services

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
  //case class Update(message: MarketMessage, candleData: JsArray)
}

class MarketService(val marketName: String) extends Actor
  with ActorLogging {
  override def preStart() = {
    //eventBus.subscribe(self, PoloniexEventBus.BTCPrice)
  }
  override def postStop() = {
    //eventBus.unsubscribe(self, PoloniexEventBus.BTCPrice)
    //log.info(s"Shutdown $marketName service")
  }

  def receive: Receive = {
    case update: MarketUpdate =>
      if (marketName == "BTC-XLM") {
        println(s"$update")
      }
  }
}

