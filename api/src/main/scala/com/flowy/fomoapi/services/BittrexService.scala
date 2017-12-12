package com.flowy.fomoapi.services

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.stream.ActorMaterializer
import com.flowy.bexchange.ExchangeService.GetMarkets
import com.flowy.bexchange.MarketTradeService.{DeleteTrade, PostTrade, UpdateTrade}
import com.flowy.common.utils.sql.SqlDatabase
import redis.RedisClient

import scala.concurrent.ExecutionContext


object BittrexService {

  def props(sqlDatabase: SqlDatabase, redis: RedisClient)
           (implicit executionContext: ExecutionContext,
            system: ActorSystem,
            materializer: ActorMaterializer) =
    Props(new BittrexService(sqlDatabase, redis))
}


class BittrexService(sqlDatabase: SqlDatabase, redis: RedisClient)(implicit executionContext: ExecutionContext,
                                                                   system: ActorSystem,
                                                                   materializer: ActorMaterializer)  extends Actor with ActorLogging {

  // activate the extension
  val mediator = DistributedPubSub(context.system).mediator

  override def preStart = {
    log.info("started the bittrex service")
  }

  override def postStop = {
    log.info("shutdown the bittrex service")
  }

  def receive = {
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
  }
}
