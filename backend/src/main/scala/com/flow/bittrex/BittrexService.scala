package com.flow.bittrex

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.flow.bittrex.api.Bittrex.{MarketResponse, MarketResult}
import com.flow.bittrex.api.BittrexClient
import com.flow.marketmaker.MarketEventBus
import com.flow.marketmaker.database.postgres.{SqlMarketUpdateDao, SqlTheEverythingBagelDao}
import com.flow.marketmaker.models.MarketStructures.MarketUpdate
import com.flow.marketmaker.services.MarketService
import com.flow.marketmaker.services.MarketService.PostTrade
import com.softwaremill.bootzooka.common.sql.SqlDatabase
import redis.RedisClient

import scala.concurrent.ExecutionContext


object BittrexService {

  def props(sqlDatabase: SqlDatabase, redis: RedisClient)
           (implicit executionContext: ExecutionContext,
            system: ActorSystem,
            materializer: ActorMaterializer) =
    Props(new BittrexService(sqlDatabase, redis))

  case class GetMarkets(filterName : Option[String] = None)
}


class BittrexService(sqlDatabase: SqlDatabase, redis: RedisClient)(implicit executionContext: ExecutionContext,
                                                                   system: ActorSystem,
                                                                   materializer: ActorMaterializer)  extends Actor with ActorLogging {

  import BittrexService._

  // TODO this is probably bad practice we should only need one instance
  // of the bagel - maybe the marketupdatedao should be merged into the everythingbagel?
  lazy val bagel = new SqlTheEverythingBagelDao(sqlDatabase)
  lazy val marketUpdateDao = new SqlMarketUpdateDao(sqlDatabase)
  val bittrexEventBus = new MarketEventBus("bittrex")
  val bittrexFeed = system.actorOf(BittrexSignalrActor.props(bittrexEventBus, marketUpdateDao), name = "bittrex.websocket")

  val bittrexClient = new BittrexClient()

  // a list of open and available trading markets on Bittrex
  var marketList: List[MarketResult] = List[MarketResult]()

  // map of marketName (BTC-ANT) to actor ref for MarketService
  val marketServices = scala.collection.mutable.Map[String, ActorRef]()


  override def preStart = {
    bittrexClient.publicGetMarkets().map { response: MarketResponse =>
      response.result match {
        case Some(list) =>
          marketList = list
        case None =>
          log.warning("could not receive market list from bittrex")
      }
    }

    log info "subscribed to bittrex socket updates"
    bittrexEventBus.subscribe(self, "updates")
  }

  override def postStop = {
    bittrexEventBus.unsubscribe(self, "updates")
  }

  def receive = {
    /*********************************************************************
      * Get market info based on a filter term
      * example filter term can be "btc-neo" or
      * full market name
      * Response with List[MarketResult]
      ********************************************************************/
    case GetMarkets(filter) =>
      filter match {
        case Some(filter) =>
          val mrks = marketList.filter( m =>
            m.MarketCurrency.toLowerCase().contains(filter.toLowerCase()) ||
            m.MarketCurrencyLong.toLowerCase().contains(filter.toLowerCase()) ||
            m.MarketName.toLowerCase().contains(filter.toLowerCase())
          )
          // only send markets that we have market actors for
          sender ! mrks.filter( m => marketServices.contains(m.MarketName))

        case None =>
          sender ! marketList.filter( m => marketServices.contains(m.MarketName))
      }

    /*********************************************************************
      * Post a trade to the market.
      ********************************************************************/
    case PostTrade(user, request, _) =>

      (marketList.find( m => m.MarketName.toLowerCase() == request.marketName.toLowerCase()),
        marketServices.get(request.marketName)) match {
        case (Some(mResult), Some(actor)) =>
          // gen uuids for all buy conditions
          val bconditions = request.buyConditions.copy(conditions =
            request.buyConditions.conditions.map( c => c.copy(id = Some(UUID.randomUUID()))))

          // gen uuids for all sell conditions
          val sconditions = request.sellConditions match {
            case Some(condArray) =>
              Some(condArray.copy(conditions =
                condArray.conditions.map( c => c.copy(id = Some(UUID.randomUUID())))))
            case None => None
          }

          val newRequest = request.copy(baseCurrencyAbbrev = Some(mResult.BaseCurrency),
            baseCurrencyName = Some(mResult.BaseCurrencyLong),
            marketCurrencyAbbrev = Some(mResult.MarketCurrency),
            marketCurrencyName = Some(mResult.MarketCurrencyLong),
            buyConditions = bconditions,
            sellConditions = sconditions
          )

          val newTrade = PostTrade(user, newRequest, Some(sender()))
          actor ! newTrade

        case _ =>
          log.warning(s"PostTrade - market not found! ${request.marketName}")
          sender ! false
      }

    /*********************************************************************
      * ship market update to correct market actor
      ********************************************************************/
    case update: MarketUpdate =>
      val marketName = update.MarketName

      // first time seeing this market name?
      if (!marketServices.contains(marketName)) {

        // fire up a new actor for this market
        marketServices += marketName -> context.actorOf(MarketService.props(marketName, bagel, redis), marketName)

        // send a message to the retriever to get the candle data from Poloniex
        // if the 24 hour baseVolume from this update is greater than our threshold
        //eventBus.publish(MarketEvent(NewMarket, QueueMarket(marketName)))
        //candleService ! QueueMarket(marketName)
      }

      //forward message to market service actor
      marketServices(marketName) ! update
  }
}
