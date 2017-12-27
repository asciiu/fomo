package com.flowy.fomoapi.services

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, Unsubscribe}
import akka.stream.ActorMaterializer
import com.flowy.bexchange.ExchangeService.GetMarkets
import com.flowy.bexchange.MarketTradeService.{DeleteTrade, PostTrade, UpdateTrade}
import com.flowy.common.models.MarketStructures.MarketUpdate
import com.flowy.common.models.{Exchange, PriceCheck}
import com.flowy.common.utils.sql.SqlDatabase
import redis.RedisClient

import scala.concurrent.ExecutionContext


object BittrexService {

  def props(sqlDatabase: SqlDatabase, redis: RedisClient)
           (implicit executionContext: ExecutionContext,
            system: ActorSystem,
            materializer: ActorMaterializer) =
    Props(new BittrexService(sqlDatabase, redis))

  case class GetPrices(marketNames: Seq[String])
}


class BittrexService(sqlDatabase: SqlDatabase, redis: RedisClient)(implicit executionContext: ExecutionContext,
                                                                   system: ActorSystem,
                                                                   materializer: ActorMaterializer)  extends Actor with ActorLogging {
  import BittrexService._

  val priceLineDotComForLeo = scala.collection.mutable.Map[String, BigDecimal]()

  // activate the extension
  val mediator = DistributedPubSub(context.system).mediator

  override def preStart = {
    log.info("started the bittrex service")
    mediator ! Subscribe("MarketUpdate", self)
  }

  override def postStop = {
    log.info("shutdown the bittrex service")
    mediator ! Unsubscribe("MarketUpdate", self)
  }

  def receive = {
    case GetPrices(markets) =>
      // sends Seq(name, price)
      if (markets.isEmpty) {
        val prices = priceLineDotComForLeo.map { x => PriceCheck(Exchange.Bittrex, x._1, x._2) }
        sender ! prices.toSeq
      } else {
        val prices = priceLineDotComForLeo.filter(p => markets.contains(p._1)).map { x => PriceCheck(Exchange.Bittrex, x._1, x._2) }
        sender ! prices.toSeq
      }

    case getMarkets: GetMarkets =>
      log.info(s"get markets")
      mediator ! Publish("GetMarkets", getMarkets.copy(leSender = Some(sender())))

    case postTrade: PostTrade =>
      log.info(s"post trade ${postTrade}")
      mediator ! Publish("PostTrade", postTrade.copy(senderOpt = Some(sender())))

    case updateTrade: UpdateTrade =>
      log.info(s"update trade ${updateTrade}")
      mediator ! Publish("UpdateTrade", updateTrade.copy(senderOpt = Some(sender())))

    case deleteTrade: DeleteTrade =>
      log.info(s"delete trade ${deleteTrade}")
      mediator ! Publish("DeleteTrade", deleteTrade.copy(senderOpt = Some(sender())))

    case update: MarketUpdate =>
      priceLineDotComForLeo += update.MarketName -> update.Last
  }
}
