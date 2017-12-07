package com.flowy.notification

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import com.flowy.common.database.MarketUpdateDao
import scala.concurrent.ExecutionContext


object NotificationActor {

  def props(marketUpdateDao: MarketUpdateDao)(implicit context: ExecutionContext,
                                              system: ActorSystem,
                                              materializer: ActorMaterializer): Props =
    Props(new NotificationActor(marketUpdateDao))
}


class NotificationActor(marketUpdateDao: MarketUpdateDao)
                         (implicit executionContext: ExecutionContext,
                          system: ActorSystem,
                          materializer: ActorMaterializer) extends Directives
  with Actor with ActorLogging {

  override def preStart() = {
  }


  override def postStop() = {
    log.info("notification system shutdown")
  }


  // implements empty receive for actor
  def receive = {
    case x =>
      log.warning(s"received unknown $x")
  }
}
