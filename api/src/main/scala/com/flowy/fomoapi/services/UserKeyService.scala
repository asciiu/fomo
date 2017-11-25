package com.flowy.fomoapi.services

import java.util.UUID

import akka.stream.ActorMaterializer
import com.flowy.fomoapi.models.UserKey
import com.flowy.fomoapi.database.dao.UserKeyDao
import com.flowy.marketmaker.api.{Auth, BittrexClient}
import com.flowy.marketmaker.api.Bittrex.BalancesResponse

import scala.concurrent.{ExecutionContext, Future}

class UserKeyService(userKeyDao: UserKeyDao)(implicit ec: ExecutionContext, materializer: ActorMaterializer) {

  lazy val bittrexClient = new BittrexClient()

  def addUserKey(userId: UUID, exchange: String, key: String, secret: String, description: String): Future[Either[String, UserKey]] = {
    userKeyDao.findByKeyPair(key, secret).flatMap { optKey =>
      optKey match {
        case Some(key) =>
          Future.successful(Left("user key already exists"))
        case None =>
          val newUserKey = UserKey.withRandomUUID(userId, exchange, key, secret, description)

          getBalances(userId, Auth(key, secret)).map { response =>
            response.message match {
              case "APIKEY_INVALID" =>
                Left("invalid key")
              case _ =>
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
          userKeyDao.updateKey(ukey)
          true
      }
    }
  }

  def remove(userId: UUID, exchange: String): Future[Boolean] = {
    // TODO cancel all trades
    userKeyDao.remove(userId, exchange)
  }

  def getUserKey(userId: UUID, exchange: String): Future[Option[UserKey]] = {
    userKeyDao.findByUserId(userId, exchange)
  }

  def getBalances(userId: UUID, auth: Auth) = {
    bittrexClient.accountGetBalances(auth)
  }
}
