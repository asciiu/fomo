package com.flowy.cache

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSubMediator.Unsubscribe
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.stream.ActorMaterializer
import com.flowy.common.api.{Auth, BittrexClient}
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models.{ApiKeyStatus, Balance, UserKey}
import java.util.UUID

import language.postfixOps
import redis.RedisClient

import scala.concurrent.ExecutionContext

object CacheService {
  def props(bagel: TheEverythingBagelDao, redis: RedisClient)
           (implicit executionContext: ExecutionContext,
            system: ActorSystem,
            materializer: ActorMaterializer) =
    Props(new CacheService(bagel, redis))

  case class CacheBittrexWallets(userId: UUID, auth: Auth)
  case class CacheBittrexBalances(userId: UUID, balances: Seq[Balance])
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
    mediator ! Subscribe("CacheBittrexBalances", self)

    bagel.userKeyDao.findAllWithStatus(ApiKeyStatus.Verified).map {
      case keys: Seq[UserKey] if keys.length > 0 =>
        keys.foreach { key =>
          cacheUserWallets(key)
        }
      case _ =>
        log.warning("found 0 verified api keys")
    }
  }

  override def postStop(): Unit = {
    mediator ! Unsubscribe("CacheBittrexBalances", self)
  }

  def receive = {

    case SubscribeAck(Subscribe("CacheBittrexBalances", None, `self`)) ⇒
      log.info("subscribed to CacheBittrexBalances commands")



    /**
      * Cache the bittrex wallets.
      * returns Future[Boolean] - true if cached false if no cache
      */
    case CacheBittrexWallets(userId, auth) =>
      //cacheUserWallets(userId, auth)

    /**
      * Cache the bittrex balances
      */
    case CacheBittrexBalances(userId, balances) =>
      cacheUserBalances(userId, balances)
  }


  // TODO response to sender with boolean
  private def cacheUserBalances(userId: UUID, balances: Seq[Balance]) = {
    log.info(s"caching balances for userId: ${userId}")

    balances.foreach { currency =>

      val key = s"userId:${userId}:bittrex:${currency.Currency}"
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
  }


  // TODO response to sender with boolean
  private def cacheUserWallets(ukey: UserKey) = {
    val auth = Auth(ukey.id, ukey.key, ukey.secret)

    bittrexClient.accountGetBalances(auth).map { authResponse =>
      authResponse.response.result match {
        case Some(balances) =>
          log.info(s"verified bittrex key for userId: ${ukey.userId}")

          cacheUserBalances(ukey.userId, balances)

        case None =>
          bagel.userKeyDao.updateKey(ukey.copy( status = ApiKeyStatus.Invalid ))
          log.info(s"invalid key or zero balance using bittrex key for userId: ${ukey.userId}")
      }
    }
  }
}

