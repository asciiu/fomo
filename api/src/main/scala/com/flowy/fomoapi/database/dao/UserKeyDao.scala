package com.flowy.fomoapi.database.dao

import java.util.UUID
import com.flowy.fomoapi.models.UserKey
import scala.concurrent.Future

trait UserKeyDao {
  def add(userKey: UserKey): Future[Option[UserKey]]
  def findById(keyId: UUID): Future[Option[UserKey]]
  def findByKeyPair(key: String, secret: String): Future[Option[UserKey]]
  def findByUserId(userId: UUID, exchangeName: String): Future[Option[UserKey]]
  def findByUserIdAndKey(userId: UUID, key: String): Future[Option[UserKey]]

  def updateKey(ukey: UserKey): Future[Boolean]
  def remove(userId: UUID, exchange: String): Future[Boolean]
}

