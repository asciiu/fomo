package com.flow.bittrex

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.flow.bittrex.api.Bittrex.{MarketResponse, MarketResult}
import com.flow.bittrex.api.BittrexClient
import com.flow.marketmaker.MarketEventBus
import com.flow.marketmaker.database.postgres.SqlMarketUpdateDao
import com.flow.marketmaker.services.MarketService.CreateOrder
import com.flow.marketmaker.services.services.actors.MarketSupervisor
import com.flow.marketmaker.services.services.actors.MarketSupervisor.GetMarketActorRef
import com.softwaremill.bootzooka.common.sql.SqlDatabase
import redis.RedisClient

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._


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

  lazy val marketUpdateDao = new SqlMarketUpdateDao(sqlDatabase)
  val bittrexEventBus = new MarketEventBus("bittrex")
  val bittrexMarketSuper = system.actorOf(MarketSupervisor.props(bittrexEventBus, sqlDatabase, redis))
  val bittrexFeed = system.actorOf(BittrexSignalrActor.props(bittrexEventBus, marketUpdateDao), name = "bittrex.websocket")

  val bittrexClient = new BittrexClient()

  // a list of open and available trading markets on Bittrex
  var marketList: List[MarketResult] = List[MarketResult]()

  override def preStart = {
    bittrexClient.publicGetMarkets().map { response: MarketResponse =>
      response.result match {
        case Some(list) =>
          marketList = list
        case None =>
          log.warning("could not receive market list from bittrex")
      }
    }
  }

  def receive = {
    /**
      * Get market info based on a filter term
      */
    case GetMarkets(filter) =>
      filter match {
        case Some(filter) =>
          val mrks = marketList.filter( m =>
            m.MarketCurrency.toLowerCase().contains(filter.toLowerCase()) ||
            m.MarketCurrencyLong.toLowerCase().contains(filter.toLowerCase()) ||
            m.MarketName.toLowerCase().contains(filter.toLowerCase())
          )
          sender ! mrks

        case None =>
          sender ! marketList
      }


    /**
      * Create an order for a user
      */
    case CreateOrder(user, buyOrder) =>
      implicit val timeout = Timeout(1.second)

      (bittrexMarketSuper ? GetMarketActorRef(buyOrder.marketName)).mapTo[Option[ActorRef]].map { opt =>

        opt match {
          case Some(marketActor) =>
            marketActor ! CreateOrder(user, buyOrder)
          case None =>
            val msg = s"market not found! ${buyOrder.marketName}"
            log.warning(msg)
        }
      }
  }
}
