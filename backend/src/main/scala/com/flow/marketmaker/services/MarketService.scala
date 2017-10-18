package com.flow.marketmaker.services

import akka.actor.{Actor, ActorLogging, Props}
import com.flow.marketmaker.models.MarketStructures.{MarketMessage, MarketUpdate}

import scala.concurrent.ExecutionContext

//package services.actors

// external
//import akka.actor.{Actor, ActorLogging, Props}
//import akka.contrib.pattern.ReceivePipeline
//import com.flow.marketmaker.models.MarketStructures.MarketMessage

//import scala.concurrent.ExecutionContext
//import scala.math.BigDecimal.RoundingMode

// internal
//import models.analytics.Archiving
//import models.analytics.individual.KitchenSink
//import models.market.MarketStructures._
//import models.poloniex.{MarketEvent, PoloniexEventBus}
//import models.strategies.BollingerAlertStrategy
//import services.DBService

object MarketService {
  def props(marketName: String)(implicit context: ExecutionContext) =
    Props(new MarketService(marketName))

  case object ReturnAllData
  case object ReturnLatestMessage
  //case class Update(message: MarketMessage, candleData: JsArray)
}

class MarketService(val marketName: String) extends Actor
  with ActorLogging {
//  with ReceivePipeline
//  with KitchenSink
//  with Archiving {
//
//  import MarketService._
//
//  //val eventBus = PoloniexEventBus()
//  //val strategy = new BollingerAlertStrategy(this)
//  //var myLastUSDPrice: BigDecimal = 0.0
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
      //println(marketName)
      //strategy.handleMessage(msg)
      //publishUpdate(msg)
    //publishUpdate(msg)
  }
}

