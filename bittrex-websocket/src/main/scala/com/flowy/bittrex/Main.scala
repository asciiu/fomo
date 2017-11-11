package com.flowy.bittrex

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.flowy.marketmaker.common.ServerConfig
import com.flowy.marketmaker.common.sql.{DatabaseConfig, SqlDatabase}
import com.flowy.marketmaker.database.postgres.{SqlMarketUpdateDao, SqlTheEverythingBagelDao}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

object Main extends App {
  // Override the configuration of the port when specified as program argument
  val port = if (args.isEmpty) "2551" else args(0)

  lazy val config = new DatabaseConfig with ServerConfig {
    override def rootConfig = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [BittrexWebSocketClient]")).
      withFallback(ConfigFactory.load())
  }

  implicit val system = ActorSystem("ClusterSystem", config.rootConfig)
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  lazy val sqlDatabase = SqlDatabase.create(config)
  lazy val bagel = new SqlTheEverythingBagelDao(sqlDatabase)
  lazy val marketUpdateDao = new SqlMarketUpdateDao(sqlDatabase)

  val bittrexFeed = system.actorOf(BittrexSignalrActor.props(marketUpdateDao), name = "bittrex.websocket")
}
