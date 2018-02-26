package com.flowy.binance

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.flowy.common.utils.ServerConfig
import com.flowy.common.utils.sql.{DatabaseConfig, SqlDatabase}
import com.flowy.common.database.postgres.{SqlMarketUpdateDao, SqlTheEverythingBagelDao}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

object Main extends App {
  // Override the configuration of the port when specified as program argument
  val port = if (args.isEmpty) "2556" else args(0)

  lazy val config = new DatabaseConfig with ServerConfig {
    override def rootConfig = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [binance-websocket]")).
      withFallback(ConfigFactory.load())
  }

  implicit val system = ActorSystem("cluster", config.rootConfig)
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  lazy val sqlDatabase = SqlDatabase.create(config)
  lazy val bagel = new SqlTheEverythingBagelDao(sqlDatabase)
  lazy val binanceDao = new SqlBinanceDao(sqlDatabase)

  val bittrexFeed = system.actorOf(BinanceWebsocket.props(binanceDao), name = "binance-websocket")
}
