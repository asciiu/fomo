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

  def upsert(ukey: UserKey): Future[Boolean] = {
    userKeyDao.upsert(ukey)
  }

  def getUserKey(userId: UUID, key: String): Future[Option[UserKey]] = {
    userKeyDao.findByUserIdAndKey(userId, key)
  }

  def getUserKeys(userId: UUID, exchangeName: String): Future[Seq[UserKey]] = {
    userKeyDao.findByUserId(userId, exchangeName)
  }
}
