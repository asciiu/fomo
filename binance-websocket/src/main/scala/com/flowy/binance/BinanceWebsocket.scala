package com.flowy.binance

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.flowy.common.database.MarketUpdateDao
import io.circe.generic.auto._, io.circe.parser._
import com.flowy.common.models.MarketStructures.MarketUpdate

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}


object BinanceWebsocket {

  def props(marketUpdateDao: MarketUpdateDao)(implicit context: ExecutionContext,
                                              system: ActorSystem,
                                              materializer: ActorMaterializer): Props =
    Props(new BinanceWebsocket(marketUpdateDao))

  object ConnectFeed
}


class BinanceWebsocket(marketUpdateDao: MarketUpdateDao)
                       (implicit executionContext: ExecutionContext,
                        system: ActorSystem,
                        materializer: ActorMaterializer) extends Directives with Actor with ActorLogging {

  import BinanceWebsocket._
  import akka.cluster.pubsub.DistributedPubSubMediator.Publish

  val cluster = Cluster(context.system)

  //val signalRServiceUrl = "http://socket.bittrex.com/signalr"
  val websocketUrl = "wss://stream.binance.com:9443/ws/!ticker@arr"

  val mediator = DistributedPubSub(context.system).mediator

  // TODO remove this as this is the temp workaround
  // until the native client is working again
  val incoming: Sink[Message, Future[Done]] =
    Sink.foreach[Message] {
      case TextMessage.Streamed(source) =>
        source.runFold("")(_ + _)(materializer).map{ str =>
          publishSummary(str)
        }(materializer.executionContext)

      case TextMessage.Strict(str) =>
        //publishSummary(str)

      case msg =>
        log.warning(s"received unknown message $msg")
    }

  // using Source.maybe materializes into a promise
  // which will allow us to complete the source later
  val flow: Flow[Message, Message, Promise[Option[Message]]] =
  Flow.fromSinkAndSourceMat(
    incoming,
    Source.maybe[Message])(Keep.right)

  private var connected = false

  override def preStart() = {
    system.scheduler.scheduleOnce(10 seconds, self, ConnectFeed)
  }

  // override postRestart so we don't call preStart and schedule a new message
  override def postRestart(reason: Throwable) = {}

  override def postStop() = {
    log.info("closed bittrex websocket")
  }

  // implements empty receive for actor
  def receive = {
    case ConnectFeed =>
      checkConnected()

    case x =>
      log.warning(s"received unknown $x")
  }

  private def checkConnected() = {
    if (!connected) {
      log.info(s"connecting to Binance websocket $websocketUrl")
      val (upgradeResponse, closed) = Http().singleWebSocketRequest(
        WebSocketRequest(websocketUrl),
        flow)

      closed.future.map { c =>
        log.info("not connected to Binance, retrying in 10 seconds")
        system.scheduler.scheduleOnce(10 seconds, self, ConnectFeed)
        connected = false
      }

      upgradeResponse.map { r =>
        if (r.response.status == StatusCodes.SwitchingProtocols) {
          log.info("connected to Binance stream")
          connected = true
        }
      }
    }
  }


  private def publishSummary(json: String): Unit = {
    // decode json string using circe
    val decodedFoo = decode[List[Binance24HrTicker]](json)

    decodedFoo match {
      case Left(x) =>
        log.info("what the fuck!")
      case Right(list) =>
        list.foreach ( println )
    }
  }

  //private def publishSummary(summary: List[BittrexNonce]) = {
  //  summary.foreach{ s =>
  //    s.Deltas.foreach{ bittrexMarketUpdate =>
  //      mediator ! Publish("MarketUpdate", bittrexMarketUpdate)
  //    }
  //  }
  //}

  //private def publishDeltas(deltas: List[MarketUpdate]) = {
  //  deltas.foreach{ bittrexMarketUpdate =>
  //    mediator ! Publish("MarketUpdate", bittrexMarketUpdate)
  //  }
  //}
}
