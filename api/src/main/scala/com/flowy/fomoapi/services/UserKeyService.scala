package com.flowy.fomoapi.services

import java.util.UUID

import akka.actor.ActorSystem
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.stream.ActorMaterializer
import com.flowy.cache.CacheService.CacheBittrexBalances
import com.flowy.common.api.Bittrex.{BalancesAuthorization, BalancesResponse}
import com.flowy.common.api.{Auth, BittrexClient}
import com.flowy.common.database.UserKeyDao
import com.flowy.common.models.{ApiKeyStatus, Exchange, UserKey}

import scala.concurrent.{ExecutionContext, Future}

class UserKeyService(userKeyDao: UserKeyDao)(implicit system: ActorSystem, ec: ExecutionContext, materializer: ActorMaterializer) {

  lazy val bittrexClient = new BittrexClient()
  lazy val mediator = DistributedPubSub(system).mediator
  lazy val defaultApiKeyId = UUID.fromString("00000000-0000-0000-0000-000000000000")

  def addUserKey(userId: UUID, exchange: Exchange.Value, key: String, secret: String, description: String): Future[Either[String, UserKey]] = {
    userKeyDao.findByKeyPair(key, secret).flatMap { optKey =>
      optKey match {
        case Some(key) =>
          Future.successful(Left("user key already exists"))
        case None =>

          getBalances(userId, Auth(defaultApiKeyId, key, secret)).map { balAuth =>
            (balAuth.response.message, balAuth.response.result) match {

              case ("", Some(balances)) =>
                mediator ! Publish("CacheBittrexBalances", CacheBittrexBalances(userId, balances))
                val newUserKey = UserKey.withRandomUUID(userId, exchange, key, secret, description, ApiKeyStatus.Verified)
                userKeyDao.add(newUserKey)
                Right(newUserKey)

              case (_, _) =>
                Left("invalid key")
            }
          }
      }
    }
  }

  def update(ukey: UserKey): Future[Option[UserKey]] = {
    getBalances(ukey.userId, Auth(ukey.id, ukey.key, ukey.secret)).flatMap { balAuth =>
      balAuth.response.message match {
        case "APIKEY_INVALID" | "INVALID_SIGNATURE" =>
          Future.successful(None)

        case "" =>
          // lets assume the DB write to status verified succeeds
          userKeyDao.updateKey(ukey.copy(status = ApiKeyStatus.Verified))

        case _ =>
          Future.successful(None)
      }
    }
  }

  def remove(userId: UUID, keyId: UUID): Future[Boolean] = {
    // TODO cancel all trades
    userKeyDao.remove(userId, keyId)
  }

  // TODO this should return a list of keys for said exchange
  def getUserKey(userId: UUID, exchange: Exchange.Value): Future[Option[UserKey]] = {
    userKeyDao.findByUserIdEx(userId, exchange)
  }

  def getUserKey(userId: UUID, keyId: UUID): Future[Option[UserKey]] = {
    userKeyDao.findByUserId(userId, keyId)
  }

  def getAllKeys(userId: UUID): Future[Seq[UserKey]] = {
    userKeyDao.findByUserId(userId)
  }

  def getBalances(userId: UUID, auth: Auth): Future[BalancesAuthorization] = {
    bittrexClient.accountGetBalances(auth).map { result =>
      println(result)
      result
    }
  }
}
