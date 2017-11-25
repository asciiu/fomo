package com.flowy.walletService

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSubMediator.Unsubscribe
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.stream.ActorMaterializer
import com.flowy.marketmaker.api.{Auth, BittrexClient}
import com.flowy.marketmaker.database.TheEverythingBagelDao
import redis.RedisClient

import language.postfixOps
import scala.concurrent.ExecutionContext

object WalletService {
  def props(bagel: TheEverythingBagelDao, redis: RedisClient)
           (implicit executionContext: ExecutionContext,
            system: ActorSystem,
            materializer: ActorMaterializer) =
    Props(new WalletService(bagel, redis))

  case class Wallet(userId: UUID, auth: Auth)
  case object Hello
}

class WalletService(bagel: TheEverythingBagelDao, redis: RedisClient)(implicit executionContext: ExecutionContext,
                                                                      system: ActorSystem,
                                                                      materializer: ActorMaterializer)  extends Actor with ActorLogging {
  import DistributedPubSubMediator.{ Subscribe, SubscribeAck }
  import WalletService._

  lazy val bittrexClient = new BittrexClient()

  // todo add redis cache
  val cluster = Cluster(context.system)

  val mediator = DistributedPubSub(context.system).mediator

  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  override def preStart(): Unit = {
    mediator ! Subscribe("Wallet", self)
    mediator ! Subscribe("WalletReserve", self)
  }

  override def postStop(): Unit = {
    mediator ! Unsubscribe("Wallet", self)
    mediator ! Unsubscribe("WalletReserve", self)
  }

  def receive = {

    case Hello =>
      println("hello from test")

    case SubscribeAck(Subscribe("Wallet", None, `self`)) ⇒
      log.info("subscribed to Wallet commands")

    case SubscribeAck(Subscribe("WalletReserve", None, `self`)) ⇒
      log.info("subscribed to WalletReserve commands")

    case Wallet(userId, auth) =>
      bittrexClient.accountGetBalances(auth).map { response =>
        response.message match {
          case "APIKEY_INVALID" =>
            Left("invalid key")
          case _ =>
            println()
        }
      }
  }
}

