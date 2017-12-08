package com.flowy.notification


import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import com.flowy.common.utils.ServerConfig
import com.malliina.push.apns._
import java.nio.file.Paths

import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck, Unsubscribe}

import scala.concurrent.ExecutionContext


object NotificationService {

  def props(config: ServerConfig)(implicit context: ExecutionContext,
                                              system: ActorSystem, materializer: ActorMaterializer): Props =
    Props(new NotificationService(config))

  case class ApplePushNotification(tokie: String, msg: String)
}


class NotificationService(config: ServerConfig)(implicit executionContext: ExecutionContext,
                          system: ActorSystem, materializer: ActorMaterializer) extends Directives with Actor with ActorLogging {

  import NotificationService._

  // required for pub sub model so this service can subscribe to messages from the cluster
  val mediator = DistributedPubSub(context.system).mediator

  val p8Path = config.getString("flowy.apns.p8Path")
  val url = getClass.getResource(p8Path)
  val keyId = config.getString("flowy.apns.keyId")
  val teamId = config.getString("flowy.apns.teamId")
  val bundleId = config.getString("flowy.apns.bundleId")

  val conf = APNSTokenConf(
    Paths.get(url.getPath()),
    KeyId(keyId),
    TeamId(teamId)
  )

  val client = APNSTokenClient(conf, isSandbox = false)
  val topic = APNSTopic(bundleId)

  override def preStart() = {
    log.info("notification service started")
    mediator ! Subscribe("ApplePushNotification", self)

    //self ! ApplePushNotification("cdfa254c91e8ee7abe4aca89e4abc8b943c675672e8d7dab6814b23cfa517ef9", "Trade SBD-BTC sold at 0.00100000")
  }


  override def postStop() = {
    log.info("notification service shutdown")
    mediator ! Unsubscribe("ApplePushNotification", self)
  }


  def receive = {
    /**
      * Send an apple push notification to device
      */
    case ApplePushNotification(tokie, msg) =>
      val deviceToken: APNSToken = APNSToken.build(tokie).get
      val message = APNSMessage.simple(msg)
      val request = APNSRequest.withTopic(topic, message)

      log.info(s"sending apns message to $tokie")

      client.push(deviceToken, request).map {
        case Left(error) =>
          log.error(s"apns message to $tokie failed $error")
        case Right(ident) =>
          log.info(s"apns message to $tokie successful $ident")
      }

    /**
      * Acknowledge the subscribe
      */
    case SubscribeAck(Subscribe("ApplePushNotification", None, `self`)) =>
      log.info("subscribed to ApplePushNotification commands")

    case x =>
      log.warning(s"notification service received unknown $x")
  }
}
