package com.flow.bittrex

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future, Promise}

object BittrexWebsocketActor {

  def props(conf: Config)(implicit context: ExecutionContext,
                          system: ActorSystem, materializer: ActorMaterializer): Props =
    Props(new BittrexWebsocketActor(conf))
}

class BittrexWebsocketActor(config: Config)
                             (implicit executionContext: ExecutionContext,
                              system: ActorSystem,
                              materializer: ActorMaterializer) extends Directives
  with BittrexJsonSupport with Actor with ActorLogging {

  import BittrexMarketEventPublisher._

  val endpoint = config.getString("bittrex.websocket")

  val publisher = system.actorOf(BittrexMarketEventPublisher.props(), name = "bittrex-publisher")

  val incoming: Sink[Message, Future[Done]] =
    Sink.foreach[Message] {
      case TextMessage.Streamed(source) =>
        source.runFold("")(_ + _)(materializer).map{ str =>
          publishSummary(str)
        }(materializer.executionContext)

      case TextMessage.Strict(str) =>
        publishSummary(str)

      case msg =>
        log.warning(s"received unknown message $msg")
    }

  // using Source.maybe materializes into a promise
  // which will allow us to complete the source later
  val flow: Flow[Message, Message, Promise[Option[Message]]] =
  Flow.fromSinkAndSourceMat(
    incoming,
    Source.maybe[Message])(Keep.right)

  val (upgradeResponse, promise) =
    Http().singleWebSocketRequest(
      WebSocketRequest(endpoint),
      flow)

  override def preStart() = {
    log.info("started bittrex websocket")
  }

  override def postStop() = {
    promise.success(None)
    log.info("closed bittrex websocket")
  }

  // implements empty receive for actor
  def receive = {
    case x =>
      log.warning(s"received unknown $x")
  }

  private def publishSummary(json: String): Unit = {
    Unmarshal(json).to[BittrexSummary].map { summary =>
      // send the summary to our publisher to process
      publisher ! MarketSummaries(summary.A)
    }
  }
}

