package com.flowy.bfeed

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest, WebSocketUpgradeResponse}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.flowy.common.database.MarketUpdateDao
import com.flowy.common.models.MarketStructures.MarketUpdate

import scala.concurrent.duration._
import com.google.gson.JsonElement
import microsoft.aspnet.signalr.client.Action

import scala.concurrent.{ExecutionContext, Future, Promise}


object BittrexSignalrActor {

  def props(marketUpdateDao: MarketUpdateDao)(implicit context: ExecutionContext,
                                              system: ActorSystem,
                                              materializer: ActorMaterializer): Props =
    Props(new BittrexSignalrActor(marketUpdateDao))

  object ConnectFeed
}


class BittrexSignalrActor(marketUpdateDao: MarketUpdateDao)
                         (implicit executionContext: ExecutionContext,
                          system: ActorSystem,
                          materializer: ActorMaterializer) extends Directives
  with BittrexJsonSupport with Actor with ActorLogging with SignalRSupport {

  import akka.cluster.pubsub.DistributedPubSubMediator.Publish
  import BittrexSignalrActor._

  val cluster = Cluster(context.system)

  val signalRServiceUrl = "http://socket.bittrex.com/signalr"

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

  private var connected = false

  override def preStart() = {
    system.scheduler.scheduleOnce(10 seconds, self, ConnectFeed)

    // TODO this is broken because of cloudfare
    // reference https://github.com/n0mad01/node.bittrex.api/issues/67
//    connectSignalR { connection =>
//      val broadcastHub = connection.createHubProxy("CoreHub")
//      //val subscription = broadcastHub.subscribe("SubscribeToSummaryDeltas")
//      val subscription = broadcastHub.subscribe("SubscribeToExchangeDeltas")
//
//      subscription.addReceivedHandler(new Action[Array[JsonElement]]() {
//        override def run(obj: Array[JsonElement]): Unit = {
//          publishSummary(obj(0).toString)
//        }
//      })
//      log.info("connected to bittrex")
//    }
  }

  // override postRestart so we don't call preStart and schedule a new message
  override def postRestart(reason: Throwable) = {}

  override def postStop() = {
    //disconnectSignalR()
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
      //log.info("connecting to resident bittrex feed")
      val (upgradeResponse, closed) = Http().singleWebSocketRequest(
        WebSocketRequest("ws://localhost:9090"),
        flow)

      closed.future.map { c =>
        //log.info("not connected retrying in 10 seconds")
        system.scheduler.scheduleOnce(10 seconds, self, ConnectFeed)
        connected = false
      }

      upgradeResponse.map { r =>
        if (r.response.status == StatusCodes.SwitchingProtocols) {
          log.info("connected to resident bittrex feed")
          connected = true
        }
      }
    }
  }


  private def publishSummary(json: String): Unit = {
    Unmarshal(json).to[BittrexSummary].map { summary =>
      summary.A.foreach { nonce =>
        marketUpdateDao.insert(nonce.Deltas)

        publishDeltas(nonce.Deltas)
      }
    }
  }

  private def publishSummary(summary: List[BittrexNonce]) = {
    summary.foreach{ s =>
      s.Deltas.foreach{ bittrexMarketUpdate =>
        mediator ! Publish("MarketUpdate", bittrexMarketUpdate)
      }
    }
  }

  private def publishDeltas(deltas: List[MarketUpdate]) = {
    deltas.foreach{ bittrexMarketUpdate =>
      mediator ! Publish("MarketUpdate", bittrexMarketUpdate)
    }
  }
}
