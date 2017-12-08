package com.flowy.notification


import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import com.flowy.common.utils.ServerConfig
import com.malliina.push.apns._
import java.nio.file.Paths
import scala.concurrent.ExecutionContext


object NotificationService {

  def props(config: ServerConfig)(implicit context: ExecutionContext,
                                              system: ActorSystem, materializer: ActorMaterializer): Props =
    Props(new NotificationService(config))

  case class SendAlert(tokie: String, msg: String)
}


class NotificationService(config: ServerConfig)(implicit executionContext: ExecutionContext,
                          system: ActorSystem, materializer: ActorMaterializer) extends Directives with Actor with ActorLogging {

  import NotificationService._


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
    self ! SendAlert("cdfa254c91e8ee7abe4aca89e4abc8b943c675672e8d7dab6814b23cfa517ef9", "Trade SBD-BTC sold at 0.00100000")
  }


  override def postStop() = {
    log.info("notification service shutdown")
  }


  def receive = {
    /**
      * Send an alert to device with token
      */
    case SendAlert(tokie, msg) =>
      val deviceToken: APNSToken = APNSToken.build(tokie).get
      val message = APNSMessage.simple(msg)
      val request = APNSRequest.withTopic(topic, message)

      client.push(deviceToken, request).map {
        case Left(error) =>
          println(s"ERROR: $error")
        case Right(ident) =>
          println(s"IDENT: $ident")
      }

    case x =>
      log.warning(s"notification service received unknown $x")
  }
}
