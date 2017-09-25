package com.flow.bittrex

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future, Promise}


class BittrexWebsocketService(config: Config)
                             (implicit executionContext: ExecutionContext,
                              system: ActorSystem,
                              materializer: ActorMaterializer) extends Directives with BittrexJsonSupport {

  val endpoint = config.getString("bittrex.websocket")
  val incoming: Sink[Message, Future[Done]] =
    Sink.foreach[Message] {
      case TextMessage.Streamed(source) =>
        source.runFold("")(_ + _)(materializer).map{ x =>
          Unmarshal(x).to[BittrexSummary].map { t =>
            println(t)
          }
        }(materializer.executionContext)
      case x =>
        //Unmarshal(x).to[BittrexSummary].map { t =>
        //  println(t)
        //}
        println(s"What is this? ${x}")
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

  def connect(): Unit = {
  }

  def disconnect() = {
    promise.success(None)
  }
}

