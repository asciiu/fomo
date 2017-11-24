package com.flowy.fomoapi.services

import java.util.UUID

import com.flowy.fomoapi.models.UserKey
import com.flowy.fomoapi.database.dao.UserKeyDao

import scala.concurrent.{ExecutionContext, Future}

class UserKeyService(userKeyDao: UserKeyDao)(implicit ec: ExecutionContext) {

  def addUserKey(userId: UUID, exchange: String, key: String, secret: String, description: String): Future[Either[String, UserKey]] = {
    userKeyDao.findByKeyPair(key, secret).map { optKey =>
      optKey match {
        case Some(key) =>
          Left("user key already exists")
        case None =>
          val newUserKey = UserKey.withRandomUUID(userId, exchange, key, secret, description)
          userKeyDao.add(newUserKey)
          Right(newUserKey)
      }
    }
  }

  def update(ukey: UserKey): Future[Boolean] = {
    userKeyDao.updateKey(ukey)
  }

  def remove(userId: UUID, exchange: String): Future[Boolean] = {
    // TODO cancel all trades
    userKeyDao.remove(userId, exchange)
  }

  def getUserKey(userId: UUID, exchange: String): Future[Option[UserKey]] = {
    userKeyDao.findByUserId(userId, exchange)
  }
}
