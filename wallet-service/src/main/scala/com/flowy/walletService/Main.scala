package com.flowy.walletService

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.flowy.marketmaker.common.ServerConfig
import com.flowy.marketmaker.common.sql.{DatabaseConfig, SqlDatabase}
import com.flowy.marketmaker.database.postgres.SqlTheEverythingBagelDao
import com.typesafe.config.ConfigFactory
import redis.RedisClient

import scala.concurrent.ExecutionContext


object Main extends App {

  val port = if (args.isEmpty) "2555" else args(0)

  lazy val config = new DatabaseConfig with ServerConfig {
    override def rootConfig = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [WalletService]")).
      withFallback(ConfigFactory.load())
  }

  implicit val system = ActorSystem("ClusterSystem", config.rootConfig)
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  lazy val redis = new RedisClient()
  lazy val sqlDatabase = SqlDatabase.create(config)
  lazy val bagel = new SqlTheEverythingBagelDao(sqlDatabase)

  system.actorOf(WalletService.props(bagel, redis), name = "wallet-service")
}
