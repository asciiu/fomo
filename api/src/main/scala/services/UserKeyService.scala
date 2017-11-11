package services

import java.util.UUID

import database.dao.UserKeyDao
import models.UserKey
import scala.concurrent.{ExecutionContext, Future}

class UserKeyService(userKeyDao: UserKeyDao)(implicit ec: ExecutionContext) {

  def addUserKey(userId: UUID, key: String, secret: String, description: String): Future[Either[String, UserKey]] = {
    userKeyDao.findByKeyPair(key, secret).map { optKey =>
      optKey match {
        case Some(key) =>
          Left("user key already exists")
        case None =>
          val newUserKey = UserKey.withRandomUUID(userId, key, secret, description)
          userKeyDao.add(newUserKey)
          Right(newUserKey)
      }
    }
  }

  def getUserKey(userId: UUID, key: String): Future[Option[UserKey]] = {
    userKeyDao.findByUserId(userId, key)
  }
}
