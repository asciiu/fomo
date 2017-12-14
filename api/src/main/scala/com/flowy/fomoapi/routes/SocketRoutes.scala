package com.flowy.fomoapi.routes

import akka.NotUsed
import akka.actor.ActorSystem
import com.flowy.fomoapi.services.{MarketUpdateService, Subscriber, UserKeyService, UserService}
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.user.api._
import com.typesafe.scalalogging.StrictLogging
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import akka.actor._


trait SocketRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  def userService: UserService
  def userKeyService: UserKeyService
  def system: ActorSystem

  val marketUpdater = system.actorOf(MarketUpdateService.props())
  val socketRoutes = logRequestResult("SocketRoutes") {
    marketUpdates
  }

  def marketUpdates =
    path("markets") {
      get {
        handleWebSocketMessages(subscribe)
      }
    }


  def subscribe(): Flow[Message, Message, NotUsed] = {
    // new connection - new user actor
    val subscriber = system.actorOf(Subscriber.props(marketUpdater))

    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message].map {
        // transform websocket message to domain message
        case TextMessage.Strict(text) => Subscriber.IncomingMessage(text)
      }.to(Sink.actorRef[Subscriber.IncomingMessage](subscriber, PoisonPill))

    val outgoingMessages: Source[Message, NotUsed] =
      Source.actorRef[Subscriber.OutgoingMessage](1000, OverflowStrategy.fail)
        .mapMaterializedValue { outActor =>
          // give the user actor a way to send messages out
          subscriber ! Subscriber.Connected(outActor)
          NotUsed
        }.map(
        // transform domain message to web socket message
        (outMsg: Subscriber.OutgoingMessage) => TextMessage(outMsg.text))

    // then combine both to a flow
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }
}
