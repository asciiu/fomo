package com.flowy.bexchange

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.flowy.bfeed.BittrexSignalrActor
import com.flowy.cache.CacheService
import com.flowy.common.utils.ServerConfig
import com.flowy.common.utils.sql.{DatabaseConfig, SqlDatabase}
import com.flowy.common.database.postgres.{SqlMarketUpdateDao, SqlTheEverythingBagelDao}
import com.flowy.notification.NotificationService
import com.typesafe.config.ConfigFactory
import com.flowy.trailstop.TrailingStopLossService
import redis.RedisClient

import scala.concurrent.ExecutionContext


object Main extends App {
  // Override the configuration of the port when specified as program argument
  val port = if (args.isEmpty) "2551" else args(0)

  lazy val config = new DatabaseConfig with ServerConfig {
    override def rootConfig = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [bfeed]")).
      withFallback(ConfigFactory.load())
  }

  implicit val system = ActorSystem("cluster", config.rootConfig)
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  lazy val redis = new RedisClient()

  lazy val sqlDatabase = SqlDatabase.create(config)
  lazy val bagel = new SqlTheEverythingBagelDao(sqlDatabase)
  lazy val marketUpdateDao = new SqlMarketUpdateDao(sqlDatabase)

  system.actorOf(ExchangeService.props(bagel, redis), name = "bexchange")
  system.actorOf(BittrexSignalrActor.props(marketUpdateDao), name = "bfeed")
  system.actorOf(CacheService.props(bagel, redis), name = "cache")
  system.actorOf(Props[TrailingStopLossService], name = "trailstop")
  system.actorOf(NotificationService.props(config), name = "notification")
}
