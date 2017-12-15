package com.flowy.bexchange

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, Unsubscribe}
import akka.stream.ActorMaterializer
import com.flowy.common.api.Bittrex.{MarketResponse, MarketResult}
import com.flowy.common.api.BittrexClient
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models.MarketStructures.MarketUpdate
import redis.RedisClient
import scala.concurrent.ExecutionContext


object ExchangeService {

  def props(bagel: TheEverythingBagelDao, redis: RedisClient)
           (implicit executionContext: ExecutionContext,
            system: ActorSystem,
            materializer: ActorMaterializer) =
    Props(new ExchangeService(bagel, redis))

  case class GetMarkets(filterName : Option[String] = None, leSender: Option[ActorRef] = None)
}

/**
  * Represents the entire exchange. Each market will be serviced by its own actor.
  * @param bagel
  * @param redis
  * @param executionContext
  * @param system
  * @param materializer
  */
class ExchangeService(bagel: TheEverythingBagelDao, redis: RedisClient)(implicit executionContext: ExecutionContext,
                                                                    system: ActorSystem,
                                                                    materializer: ActorMaterializer)  extends Actor with ActorLogging {

  import ExchangeService._
  import MarketTradeService._

  // need to pull info via bittrex ReST API
  val bittrexClient = new BittrexClient()

  // a list of open and available trading markets on Bittrex
  var marketList: List[MarketResult] = List[MarketResult]()

  // map of marketName (BTC-ANT) to actor ref for MarketService
  val marketServices = scala.collection.mutable.Map[String, ActorRef]()

  val mediator = DistributedPubSub(context.system).mediator

  override def preStart = {
    bittrexClient.publicGetMarkets().map { response: MarketResponse =>
      response.result match {
        case Some(list) =>
          marketList = list
        case None =>
          log.warning("could not receive market list from bittrex")
      }
    }
    mediator ! Subscribe("GetMarkets", self)
    mediator ! Subscribe("MarketUpdate", self)
    mediator ! Subscribe("PostTrade", self)
    mediator ! Subscribe("UpdateTrade", self)
    mediator ! Subscribe("DeleteTrade", self)
  }

  override def postStop(): Unit = {
    mediator ! Unsubscribe("GetMarkets", self)
    mediator ! Unsubscribe("MarketUpdate", self)
    mediator ! Unsubscribe("PostTrade", self)
    mediator ! Unsubscribe("UpdateTrade", self)
    mediator ! Unsubscribe("DeleteTrade", self)
  }

  def receive = {
    /*********************************************************************
      * Get market info based on a filter term
      * example filter term can be "btc-neo" or
      * full market name
      * Response with List[MarketResult]
      ********************************************************************/
    case GetMarkets(filterOpt, senderOpt) =>
      (filterOpt, senderOpt) match {
        case (Some(filter), Some(s)) =>
          val mrks = marketList.filter( m =>
            m.marketCurrency.toLowerCase().contains(filter.toLowerCase()) ||
            m.marketCurrencyLong.toLowerCase().contains(filter.toLowerCase()) ||
            m.marketName.toLowerCase().contains(filter.toLowerCase())
          )
          // only send markets that we have market actors for
          s ! mrks.filter( m => marketServices.contains(m.marketName))

        case (None, Some(s)) =>
          s ! marketList.filter( m => marketServices.contains(m.marketName))

        case _ =>
          log.warning("GetMarkets does not contain a sender ref!")
      }

    /*********************************************************************
      * Update a trade.
      ********************************************************************/
    case DeleteTrade(trade, senderOpt) =>
      marketServices.get(trade.marketName) match {
        case Some(actor) =>
          actor ! DeleteTrade(trade, senderOpt)

        case None =>
          log.warning(s"DeleteTrade - market not found! ${trade.marketName}")
          senderOpt match {
            case Some(actor) =>
              actor ! None
            case None =>
              sender ! None
          }
      }


    /*********************************************************************
      * Post a trade to the market.
      ********************************************************************/
    case PostTrade(user, request, senderOpt) =>
      (marketList.find( m => m.marketName.toLowerCase() == request.marketName.toLowerCase()), marketServices.get(request.marketName)) match {
        case (Some(mResult), Some(actor)) =>
          // we must match on Some(mResult), Some(actor) to ensure we have the correct
          // resources for the marketservice

          val newRequest = request.copy(baseCurrencyAbbrev = Some(mResult.baseCurrency),
            baseCurrencyName = Some(mResult.baseCurrencyLong),
            marketCurrencyAbbrev = Some(mResult.marketCurrency),
            marketCurrencyName = Some(mResult.marketCurrencyLong)
          )

          // send the request with completed names to the market service
          val newTrade = PostTrade(user, newRequest, senderOpt)
          actor ! newTrade

        case _ =>
          log.warning(s"PostTrade - ${request.marketName} not found!")
          senderOpt match {
            case Some(actor) =>
              actor ! None
            case None =>
              sender ! None
          }
      }

    /*********************************************************************
      * Update a trade.
      ********************************************************************/
    case uptrade: UpdateTrade =>
      marketServices.get(uptrade.request.marketName) match {
        case Some(actor) =>
          actor ! uptrade

        case None =>
          log.warning(s"UpdateTrade - market not found! ${uptrade.request.marketName}")
          uptrade.senderOpt match {
            case Some(actor) =>
              actor ! None
            case None =>
              sender ! None
          }
      }


    /*********************************************************************
      * ship market update to correct market actor
      ********************************************************************/
    case update: MarketUpdate =>
      val marketName = update.MarketName

      // first time seeing this market name?
      if (!marketServices.contains(marketName)) {

        // fire up a new actor for this market
        marketServices += marketName -> context.actorOf(MarketTradeService.props(marketName, bagel, redis), marketName)

        log.info(s"MarketTradeService started for ${marketName}")

        // send a message to the retriever to get the candle data from Poloniex
        // if the 24 hour baseVolume from this update is greater than our threshold
        //eventBus.publish(MarketEvent(NewMarket, QueueMarket(marketName)))
        //candleService ! QueueMarket(marketName)
      }

      //forward message to market service actor
      marketServices(marketName) ! update
  }
}
