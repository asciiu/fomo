package com.flowy.trailingStop

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSubMediator.Unsubscribe
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import com.flowy.common.models.MarketStructures.MarketUpdate

import language.postfixOps
import messages.{GetStopLosses, TrailingStop}

class TrailingStopLossService extends Actor with ActorLogging{
  import DistributedPubSubMediator.{ Subscribe, SubscribeAck }

  // todo add redis cache
  val cluster = Cluster(context.system)

  val stopSells = scala.collection.mutable.Map[String, StopLossCollection]()

  val mediator = DistributedPubSub(context.system).mediator

  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  override def preStart(): Unit = {
    mediator ! Subscribe("TrailingStop", self)
    mediator ! Subscribe("MarketUpdate", self)
  }

  override def postStop(): Unit = {
    mediator ! Unsubscribe("TrailingStop", self)
    mediator ! Unsubscribe("MarketUpdate", self)
  }

  def receive = {
    case GetStopLosses(userId, marketName) =>
      stopSells.get(marketName) match {
        case Some(collection) =>
          sender ! collection.getStopLosses(userId)
        case None =>
          log.warning(s"user $userId $marketName stop losses not found")
      }

    /**
      * Adds a new trailing stop sell
      */
    case stopSell: TrailingStop =>
      stopSells.get(stopSell.marketName) match {
        case Some(collection) =>
          log.info(s"setting trailing stop ${stopSell}")
          collection.addStopLoss(stopSell)
        case None =>
          log.warning(s"market has not been received in updates yet ${stopSell}")
      }

    case SubscribeAck(Subscribe("TrailingStop", None, `self`)) ⇒
      log.info("subscribed to trailing stops")

    case SubscribeAck(Subscribe("MarketUpdate", None, `self`)) ⇒
      log.info("subscribed to market updates")

    /**
      * update state of service from market update
      */
    case update: MarketUpdate =>
      stopSells.get(update.MarketName) match {
        case Some(collection) =>
          collection.updateStopLosses(update.Last)
          collection.triggeredStopLossesRemoved(update.Last).foreach { stop =>
            log.info(s"trigger stop sell $stop")
          }
        case None =>
          val collection = new StopLossCollection(update.MarketName, update.Last)
          stopSells += (update.MarketName -> collection)
      }
  }
}

