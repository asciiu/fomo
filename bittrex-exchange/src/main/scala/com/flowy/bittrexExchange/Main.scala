package com.flowy.bittrexExchange

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.flowy.bittrexWebsocket.BittrexSignalrActor
import com.flowy.cacheService.CacheService
import com.flowy.common.utils.ServerConfig
import com.flowy.common.utils.sql.{DatabaseConfig, SqlDatabase}
import com.flowy.common.database.postgres.{SqlMarketUpdateDao, SqlTheEverythingBagelDao}
import com.typesafe.config.ConfigFactory
import com.flowy.trailingStop.TrailingStopLossService
import redis.RedisClient

import scala.concurrent.ExecutionContext


object Main extends App {
  // Override the configuration of the port when specified as program argument
  val port = if (args.isEmpty) "2551" else args(0)

  lazy val config = new DatabaseConfig with ServerConfig {
    override def rootConfig = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [bittrex-exchange]")).
      withFallback(ConfigFactory.load())
  }

  implicit val system = ActorSystem("cluster", config.rootConfig)
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  lazy val redis = new RedisClient()

  lazy val sqlDatabase = SqlDatabase.create(config)
  lazy val bagel = new SqlTheEverythingBagelDao(sqlDatabase)
  lazy val marketUpdateDao = new SqlMarketUpdateDao(sqlDatabase)

  system.actorOf(ExchangeService.props(bagel, redis), name = "exchange-service")
  system.actorOf(BittrexSignalrActor.props(marketUpdateDao), name = "bittrex-feed")
  system.actorOf(CacheService.props(bagel, redis), name = "cache-service")
  system.actorOf(Props[TrailingStopLossService], name = "trailing-stop")
}
