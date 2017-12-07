package com.flowy.notification

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.flowy.common.utils.ServerConfig
import com.flowy.common.utils.sql.{DatabaseConfig, SqlDatabase}
import com.flowy.common.database.postgres.{SqlMarketUpdateDao, SqlTheEverythingBagelDao}
import com.malliina.push.apns._
import com.typesafe.config.ConfigFactory
import scala.io.Source

import scala.concurrent.{ExecutionContext, Future}

object Main extends App {
  // Override the configuration of the port when specified as program argument
  val port = if (args.isEmpty) "2556" else args(0)

  lazy val config = new DatabaseConfig with ServerConfig {
    override def rootConfig = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [bittrex-websocket]")).
      withFallback(ConfigFactory.load())
  }

  implicit val system = ActorSystem("cluster", config.rootConfig)
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  lazy val sqlDatabase = SqlDatabase.create(config)
  lazy val bagel = new SqlTheEverythingBagelDao(sqlDatabase)
  lazy val marketUpdateDao = new SqlMarketUpdateDao(sqlDatabase)

  val url = getClass.getResource("/AuthKey_LJCP6MGHV2.p8")
  val conf = APNSTokenConf(
    Paths.get(url.getPath()),
    KeyId("RGWNJ3AJG7"),
    TeamId("5MJU2NDS4K")
  )

  val client = APNSTokenClient(conf, isSandbox = false)
  val topic = APNSTopic("com.fluidmarket.fluids")
  val deviceToken: APNSToken = APNSToken.build("cdfa254c91e8ee7abe4aca89e4abc8b943c675672e8d7dab6814b23cfa517ef9").get
  val message = APNSMessage.simple("Hey, sexy!")
  val request = APNSRequest.withTopic(topic, message)
    client.push(deviceToken, request).map {
      case Left(error) =>
        println(s"ERROR: $error")
      case Right(ident) =>
        println(s"IDENT: $ident")

    }

}
