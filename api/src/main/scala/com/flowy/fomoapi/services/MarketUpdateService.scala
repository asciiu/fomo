package com.flowy.fomoapi.services

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, Unsubscribe}
import com.flowy.common.models.MarketStructures.MarketUpdate
import com.flowy.common.models.Trade
import io.circe.{Encoder, Json}
import io.circe.generic.auto._
import io.circe.syntax._


object MarketUpdateService {
  def props() = Props(new MarketUpdateService())

  case object Join
  case class UpdateMessage(message: String)
}

class MarketUpdateService extends Actor {
  import MarketUpdateService._
  var subscribers: Set[ActorRef] = Set.empty

  val mediator = DistributedPubSub(context.system).mediator

  implicit val encode: Encoder[MarketUpdate] = new Encoder[MarketUpdate] {
    final def apply(update: MarketUpdate): Json = {
      Json.obj(
        ("market", Json.fromString(update.MarketName)),
        ("last", Json.fromDoubleOrNull(update.Last))
      )
    }
  }

  override def preStart = {
    mediator ! Subscribe("MarketUpdate", self)
  }
  override def postStop = {
    mediator ! Unsubscribe("MarketUpdate", self)
  }

  def receive = {
    case Join =>
      subscribers += sender()
      // we also would like to remove the user when its actor is stopped
      context.watch(sender())

    case Terminated(user) =>
      subscribers -= user

    case msg: UpdateMessage =>
      subscribers.foreach(_ ! msg)

    case update: MarketUpdate =>
      subscribers.foreach(_ ! UpdateMessage(update.asJson.toString()))
  }
}

object Subscriber {
  def props(market: ActorRef) = Props(new Subscriber(market))
  case class Connected(outgoing: ActorRef)
  case class IncomingMessage(text: String)
  case class OutgoingMessage(text: String)
}

class Subscriber(market: ActorRef) extends Actor {
  import Subscriber._

  def receive = {
    case Connected(outgoing) =>
      context.become(connected(outgoing))
  }

  def connected(outgoing: ActorRef): Receive = {
    market ! MarketUpdateService.Join

    {
      case IncomingMessage(text) =>
        market ! MarketUpdateService.UpdateMessage(text)

      case MarketUpdateService.UpdateMessage(text) =>
        outgoing ! OutgoingMessage(text)
    }
  }

}
