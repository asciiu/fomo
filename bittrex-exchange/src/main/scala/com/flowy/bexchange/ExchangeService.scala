package com.flowy.bexchange

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, Unsubscribe}
import akka.stream.ActorMaterializer
import com.flowy.common.api.Bittrex.{MarketResponse, MarketResult}
import com.flowy.common.api.BittrexClient
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models.{Exchange, Market}
import com.flowy.common.models.MarketStructures.MarketUpdate
import redis.RedisClient

import scala.collection.mutable.ListBuffer
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
  val marketList: ListBuffer[Market] = new ListBuffer()

  // map of marketName (BTC-ANT) to actor ref for MarketService
  val marketServices = scala.collection.mutable.Map[String, ActorRef]()

  val mediator = DistributedPubSub(context.system).mediator

  override def preStart = {
    queryBittrexMarkets()

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

  private def queryBittrexMarkets() = {
    bittrexClient.publicGetMarkets().map { response: MarketResponse =>
      response.result match {
        case Some(list) =>

          list.foreach{ marketResult =>

            val marketName = marketResult.marketName

            if (!marketServices.contains(marketName)) {
              // fire up a new actor for this market
              marketServices += marketName -> context.actorOf(MarketTradeService.props(marketName, bagel, redis), marketName)

              // add this market to our lookup table if it does not already exist
              bagel.findMarketByName(Exchange.Bittrex, marketName).map {
                case Some(market) =>
                  marketList += market

                case None =>
                  val market = Market(
                    UUID.randomUUID(),
                    Exchange.Bittrex,
                    marketName,
                    marketResult.marketCurrency,
                    marketResult.marketCurrencyLong,
                    marketResult.baseCurrency,
                    marketResult.baseCurrencyLong
                  )

                  bagel.insert(market)
                  marketList += market
              }

              log.info(s"MarketTradeService started for ${marketName}")
            }
          }

        case None =>
          log.warning("could not receive market list from bittrex")
      }
    }
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
      marketServices.get(trade.info.marketName) match {
        case Some(actor) =>
          actor ! DeleteTrade(trade, senderOpt)

        case None =>
          log.warning(s"DeleteTrade - market not found! ${trade.info.marketName}")
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
      val marketName = request.marketName

      marketList.find(_.marketName == marketName) match {
        case Some(lookup) =>
          // we must match on Some(mResult), Some(actor) to ensure we have the correct
          // resources for the marketservice

          val newRequest = request.copy(baseCurrency = Some(lookup.baseCurrency),
            baseCurrencyLong = Some(lookup.baseCurrencyLong),
            marketCurrency = Some(lookup.marketCurrency),
            marketCurrencyLong = Some(lookup.marketCurrencyLong)
          )

          // send the request with completed names to the market service
          val newTrade = PostTrade(user, newRequest, senderOpt)
          marketServices(marketName) ! newTrade

        case _ =>
          log.warning(s"PostTrade - ${marketName} not found!")
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
        // startup new actor if we can
        queryBittrexMarkets()
      } else {
        //forward message to market service actor
        marketServices(marketName) ! update
      }
  }
}
