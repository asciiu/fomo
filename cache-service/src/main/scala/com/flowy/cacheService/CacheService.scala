package com.flowy.cacheService

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSubMediator.Unsubscribe
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.stream.ActorMaterializer
import com.flowy.common.Util
import com.flowy.common.api.{Auth, BittrexClient}
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models.{Exchange, UserKey}

import language.postfixOps
import redis.RedisClient

import scala.concurrent.{ExecutionContext, Future}

object CacheService {
  def props(bagel: TheEverythingBagelDao, redis: RedisClient)
           (implicit executionContext: ExecutionContext,
            system: ActorSystem,
            materializer: ActorMaterializer) =
    Props(new CacheService(bagel, redis))

  case class CacheBittrexWallets(userId: UUID, auth: Auth)
  case object Hello
}

/**
  * This service shall be the place where everything will be cached.
  * @param bagel
  * @param redis
  * @param executionContext
  * @param system
  * @param materializer
  */
class CacheService(bagel: TheEverythingBagelDao, redis: RedisClient)(implicit executionContext: ExecutionContext,
                                                                     system: ActorSystem,
                                                                     materializer: ActorMaterializer)  extends Actor with ActorLogging {
  import DistributedPubSubMediator.{ Subscribe, SubscribeAck }
  import CacheService._

  lazy val bittrexClient = new BittrexClient()

  // required for akka cluster
  val cluster = Cluster(context.system)
  // required for pub sub model so this service can subscribe to messages from the cluster
  val mediator = DistributedPubSub(context.system).mediator

  // list of redis keys
  val cachedKeys = scala.collection.mutable.ListBuffer[String]()

  override def preStart(): Unit = {
    // subscribe to cluster messages
    mediator ! Subscribe("CacheBittrexWallets", self)

    bagel.userKeyDao.findAllValidated(24).map {
      case keys: Seq[UserKey] if keys.length > 0 =>
        keys.foreach { key =>
          cacheUserWallets(key)
        }
      case _ =>
        log.warning("found 0 validated api keys")
    }
  }

  override def postStop(): Unit = {
    mediator ! Unsubscribe("CacheBittrexWallets", self)
  }

  def receive = {

    case Hello =>
      println("hello from test")

    case SubscribeAck(Subscribe("Wallet", None, `self`)) ⇒
      log.info("subscribed to Wallet commands")

    case SubscribeAck(Subscribe("WalletReserve", None, `self`)) ⇒
      log.info("subscribed to WalletReserve commands")

    /**
      * Cache the bittrex wallets.
      * returns Future[Boolean] - true if cached false if no cache
      */
    case CacheBittrexWallets(userId, auth) =>
      //cacheUserWallets(userId, auth)
  }

  private def cacheUserWallets(ukey: UserKey) = {
    val auth = Auth(ukey.key, ukey.secret)

    bittrexClient.accountGetBalances(auth).map { response =>
      response.result match {
        case Some(balances) =>
          log.info(s"validated userId: ${ukey.userId} bittrex keys")
          bagel.userKeyDao.updateKey(ukey.copy( validatedOn = Some(Util.now()) ))
          balances.foreach { currency =>

            val key = s"userId:${ukey.userId}:bittrex:${currency.Currency}"
            val futureStatus = redis.hmset[String](key,
              Map("balance" -> currency.Balance.toString,
                "available" -> currency.Available.toString,
                "pending" -> currency.Pending.toString,
                "address" -> currency.CryptoAddress.toString))

            futureStatus.map{
              case true =>
                // expire the keys after 24 hours - 86400 seconds
                redis.expire(key, 86400)
                cachedKeys += key
              case false => ???
            }
          }
        case None =>
          log.info(s"invalid key or zero balance for bittrex internal user ${ukey.userId}")
      }
    }
  }
}

