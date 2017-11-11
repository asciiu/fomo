package com.softwaremill.bootzooka

import akka.actor.{ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import services.HttpService

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Main extends App with StrictLogging {
  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port = 2553 ").
    withFallback(ConfigFactory.parseString("akka.cluster.roles = [api]")).
    withFallback(ConfigFactory.load())

  implicit val actorSystem = ActorSystem("ClusterSystem", config)
  implicit val executor: ExecutionContext = actorSystem.dispatcher
  implicit val log: LoggingAdapter = Logging(actorSystem, getClass)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val (startFuture, bl) = new HttpService().start()

  val host = bl.config.serverHost
  val port = bl.config.serverPort

  val system = bl.system

  startFuture.onComplete {
    case Success(b) =>
      logger.info(s"Server started on $host:$port")
      sys.addShutdownHook {
        b.unbind()
        bl.system.terminate()
        logger.info("Server stopped")
      }
    case Failure(e) =>
      logger.error(s"Cannot start server on $host:$port", e)
      sys.addShutdownHook {
        bl.system.terminate()
        logger.info("Server stopped")
      }
  }
}
