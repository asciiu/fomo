package com.flowy.fomoapi.services

import java.util.UUID

import akka.stream.ActorMaterializer
import com.flowy.common.api.{Auth, BittrexClient}
import com.flowy.common.database.UserKeyDao
import com.flowy.common.models.{ApiKeyStatus, Exchange, UserKey}

import scala.concurrent.{ExecutionContext, Future}

class UserKeyService(userKeyDao: UserKeyDao)(implicit ec: ExecutionContext, materializer: ActorMaterializer) {

  lazy val bittrexClient = new BittrexClient()

  def addUserKey(userId: UUID, exchange: Exchange.Value, key: String, secret: String, description: String): Future[Either[String, UserKey]] = {
    userKeyDao.findByKeyPair(key, secret).flatMap { optKey =>
      optKey match {
        case Some(key) =>
          Future.successful(Left("user key already exists"))
        case None =>
          getBalances(userId, Auth(key, secret)).map { response =>
            response.message match {
              case "APIKEY_INVALID" =>
                Left("invalid key")
              case _ =>
                val newUserKey = UserKey.withRandomUUID(userId, exchange, key, secret, description, ApiKeyStatus.Verified)
                userKeyDao.add(newUserKey)
                Right(newUserKey)
            }
          }
      }
    }
  }

  def update(ukey: UserKey): Future[Boolean] = {
    getBalances(ukey.userId, Auth(ukey.key, ukey.secret)).map { response =>
      response.message match {
        case "APIKEY_INVALID" =>
          false
        case _ =>
          userKeyDao.updateKey(ukey.copy(status = ApiKeyStatus.Verified))
          true
      }
    }
  }

  def remove(userId: UUID, exchange: Exchange.Value): Future[Boolean] = {
    // TODO cancel all trades
    userKeyDao.remove(userId, exchange)
  }

  def getUserKey(userId: UUID, exchange: Exchange.Value): Future[Option[UserKey]] = {
    userKeyDao.findByUserId(userId, exchange)
  }

  def getBalances(userId: UUID, auth: Auth) = {
    bittrexClient.accountGetBalances(auth)
  }
}
