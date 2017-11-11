package com.flowy.bittrex

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, ActorSystem, Props, RootActorPath}
import akka.cluster.{Cluster, Member}
import akka.cluster.ClusterEvent.MemberUp
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.flowy.marketmaker.database.MarketUpdateDao
import com.flowy.marketmaker.models.BittrexWebsocketClientRegistration
import com.flowy.marketmaker.models.MarketStructures.MarketUpdate
import com.google.gson.JsonElement
import microsoft.aspnet.signalr.client.Action

import scala.concurrent.ExecutionContext

object BittrexSignalrActor {

  def props(marketUpdateDao: MarketUpdateDao)(implicit context: ExecutionContext,
                                              system: ActorSystem,
                                              materializer: ActorMaterializer): Props =
    Props(new BittrexSignalrActor(marketUpdateDao))
}

class BittrexSignalrActor(marketUpdateDao: MarketUpdateDao)
                         (implicit executionContext: ExecutionContext,
                          system: ActorSystem,
                          materializer: ActorMaterializer) extends Directives
  with BittrexJsonSupport with Actor with ActorLogging with SignalRSupport {

  val cluster = Cluster(context.system)
  val signalRServiceUrl = "http://socket.bittrex.com/signalr"

  var listeners = IndexedSeq.empty[ActorSelection]

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

    cluster.subscribe(self, classOf[MemberUp])
    log.info("started bittrex websocket")
  }


  override def postStop() = {
    cluster.unsubscribe(self)
    disconnectSignalR()
    log.info("closed bittrex websocket")
  }


  // implements empty receive for actor
  def receive = {
    case MemberUp(m) =>
      register(m)

    case x =>
      log.warning(s"received unknown $x")
  }


  private def publishSummary(json: String): Unit = {
    Unmarshal(json).to[BittrexNonce].map { nonce =>
      marketUpdateDao.insert(nonce.Deltas)

      publishDeltas(nonce.Deltas)
    }
  }


  private def publishSummary(summary: List[BittrexNonce]) = {
    summary.foreach{ s =>
      s.Deltas.foreach{ bittrexMarketUpdate =>
        listeners.foreach( _ ! bittrexMarketUpdate)
      }
    }
  }


  private def publishDeltas(deltas: List[MarketUpdate]) = {
    deltas.foreach{ bittrexMarketUpdate =>
      listeners.foreach( _ ! bittrexMarketUpdate)
    }
  }

  private def register(member: Member): Unit = {

    if (member.hasRole("api")) {
      val actorRef = context.actorSelection(RootActorPath(member.address) / "user" / "bittrex")
      listeners = listeners :+ actorRef
      actorRef ! BittrexWebsocketClientRegistration
    } else if (member.hasRole("TrailingStopLossService")) {
      val actorRef = context.actorSelection(RootActorPath(member.address) / "user" / "trailing-stop")
      listeners = listeners :+ actorRef
      actorRef ! BittrexWebsocketClientRegistration
    }
  }
}
