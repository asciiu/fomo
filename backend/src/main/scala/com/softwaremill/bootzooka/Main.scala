package com.softwaremill.bootzooka

import java.util.UUID

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import com.flow.bittrex.BittrexWebsocketActor
import com.flow.marketmaker.MarketEventBus
import com.flow.marketmaker.services.services.actors.MarketSupervisor
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


object Main extends App with StrictLogging {
  implicit val actorSystem = ActorSystem("main")
  implicit val executor: ExecutionContext = actorSystem.dispatcher
  implicit val log: LoggingAdapter = Logging(actorSystem, getClass)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val bittrexEventBus = new MarketEventBus("bittrex")
  val bittrexFeed = actorSystem.actorOf(BittrexWebsocketActor.props(bittrexEventBus, ConfigFactory.load()), name = "bittrex.websocket")
  val bittrexMarketSuper = actorSystem.actorOf(MarketSupervisor.props(bittrexEventBus))

//  val userID = java.util.UUID.randomUUID()
//  Database.UserDao.add(
//    User(userID,
//      "test@test",
//      "Test",
//      "User",
//      "hash",
//      new DateTime(),
//      new DateTime()))
//
//  Database.UserDao.getById(userID).map { opt =>
//    opt match {
//      case Some(user) =>
//        println(s"#################${user.firstName}")
//        println(user.passwordHash)
//      case _ => println("nope")
//    }
//  }

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
