package com.flow.marketmaker.services

package services.actors

// external
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.flow.marketmaker.MarketEventBus
import com.flow.marketmaker.models.MarketStructures.MarketUpdate

import scala.concurrent.ExecutionContext
import scala.language.postfixOps


object MarketSupervisor {
  def props(eventBus: MarketEventBus)
           (implicit executionContext: ExecutionContext,
            system: ActorSystem) = Props(new MarketSupervisor(eventBus))

  case class GetMarketActorRef(marketName: String)
}

/**
  * This actor is reponsible for managing all poloniex markets. New
  * actors for each market are created here.
  */
class MarketSupervisor (eventBus: MarketEventBus)
                       (implicit ctx: ExecutionContext, system: ActorSystem)
  extends Actor
    with ActorLogging {

  import MarketSupervisor._

  //import PoloniexCandleRetrieverService._

  // we need this candle service to retrieve candles data from the poloniex api
  //val candleService = system.actorOf(PoloniexCandleRetrieverService.props(ws, conf))

  // keep tabs on each market ref by market name
  val markets = scala.collection.mutable.Map[String, ActorRef]()

  //val eventBus = PoloniexEventBus()

  // this rule defines a base threshold for market candle retrieval
  // candle data will be retrieved from poloniex if and only if
  // the 24 hr BTC base volume of a market is greater
  //val baseVolumeRule = conf.getInt("poloniex.candle.baseVolume").getOrElse(500)

  override def preStart() = {
    log info "subscribed to market updates"
    eventBus.subscribe(self, "updates")
  }

  override def postStop() = {
    eventBus.unsubscribe(self, "updates")
  }

  def receive = {

    /**
      * Send back an optional actor ref for the market name
      */
    case GetMarketActorRef(marketName) =>
      sender() ! markets.get(marketName)

    /**
      * ship market update to correct market actor
      */
    case update: MarketUpdate =>
      val marketName = update.MarketName
      //if (marketName.contains("USDT_BTC")) {
        //eventBus.publish(MarketEvent(PoloniexEventBus.BTCPrice, PriceUpdateBTC(msg.time, msg.last)))
      //}
        // first time seeing this market name?
      if (!markets.contains(marketName)) {

        // fire up a new actor for this market
        markets += marketName -> context.actorOf(MarketService.props(marketName), marketName)

        // send a message to the retriever to get the candle data from Poloniex
        // if the 24 hour baseVolume from this update is greater than our threshold
        //eventBus.publish(MarketEvent(NewMarket, QueueMarket(marketName)))
        //candleService ! QueueMarket(marketName)
      }

      //forward message to market service actor
      markets(marketName) ! update
  }
}


