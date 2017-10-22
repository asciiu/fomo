package com.flow.bittrex

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.flow.marketmaker.MarketEventBus
import com.flow.marketmaker.database.MarketUpdateDao
import com.google.gson.JsonElement
import microsoft.aspnet.signalr.client.Action

import scala.concurrent.{ExecutionContext, Future, Promise}

object BittrexSignalrActor {

  def props(eventBus: MarketEventBus, marketUpdateDao: MarketUpdateDao)(implicit context: ExecutionContext,
                                                system: ActorSystem, materializer: ActorMaterializer): Props =
    Props(new BittrexSignalrActor(eventBus, marketUpdateDao))
}

class BittrexSignalrActor(eventBus: MarketEventBus,
                          marketUpdateDao: MarketUpdateDao)
                         (implicit executionContext: ExecutionContext,
                          system: ActorSystem,
                          materializer: ActorMaterializer) extends Directives
  with BittrexJsonSupport with Actor with ActorLogging with SignalRSupport {

  import BittrexMarketEventPublisher._

  val signalRServiceUrl = "http://socket.bittrex.com/signalr"

  val publisher = system.actorOf(BittrexMarketEventPublisher.props(eventBus), name = "bittrex-publisher")

  override def preStart() = {
    connectSignalR { connection =>
      val broadcastHub = connection.createHubProxy("corehub")
      val subscription = broadcastHub.subscribe("updateSummaryState")

      subscription.addReceivedHandler(new Action[Array[JsonElement]]() {
        override def run(obj: Array[JsonElement]): Unit = {
          publishSummary(obj(0).toString)
        }
      })
    }

    log.info("started bittrex websocket")
  }

  override def postStop() = {
    disconnectSignalR()
    log.info("closed bittrex websocket")
  }

  // implements empty receive for actor
  def receive = {
    case x =>
      log.warning(s"received unknown $x")
  }

  private def publishSummary(json: String): Unit = {
    Unmarshal(json).to[BittrexNonce].map { nonce =>
      marketUpdateDao.insert(nonce.Deltas)
      // send the summary to our publisher to process
      publisher ! MarketDeltas(nonce.Deltas)
    }
  }
}

