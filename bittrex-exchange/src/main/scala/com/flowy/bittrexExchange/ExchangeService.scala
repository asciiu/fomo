package com.flowy.bittrexExchange

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
  }

  override def postStop(): Unit = {
    mediator ! Unsubscribe("GetMarkets", self)
    mediator ! Unsubscribe("MarketUpdate", self)
    mediator ! Unsubscribe("PostTrade", self)
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
            m.MarketCurrency.toLowerCase().contains(filter.toLowerCase()) ||
            m.MarketCurrencyLong.toLowerCase().contains(filter.toLowerCase()) ||
            m.MarketName.toLowerCase().contains(filter.toLowerCase())
          )
          // only send markets that we have market actors for
          s ! mrks.filter( m => marketServices.contains(m.MarketName))

        case (None, Some(s)) =>
          s ! marketList.filter( m => marketServices.contains(m.MarketName))

        case _ =>
          log.warning("GetMarkets does not contain a sender ref!")
      }

    /*********************************************************************
      * Post a trade to the market.
      ********************************************************************/
    case PostTrade(user, request, senderOpt) =>
      (marketList.find( m => m.MarketName.toLowerCase() == request.marketName.toLowerCase()), marketServices.get(request.marketName)) match {
        case (Some(mResult), Some(actor)) =>
          // we must match on Some(mResult), Some(actor) to ensure we have the correct
          // resources for the marketservice

          val newRequest = request.copy(baseCurrencyAbbrev = Some(mResult.BaseCurrency),
            baseCurrencyName = Some(mResult.BaseCurrencyLong),
            marketCurrencyAbbrev = Some(mResult.MarketCurrency),
            marketCurrencyName = Some(mResult.MarketCurrencyLong)
          )

          // send the request with completed names to the market service
          val newTrade = PostTrade(user, newRequest, senderOpt)
          actor ! newTrade

        case _ =>
          log.warning(s"PostTrade - market not found! ${request.marketName}")
          senderOpt match {
            case Some(actor) =>
              actor ! false
            case None =>
              sender ! false
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
