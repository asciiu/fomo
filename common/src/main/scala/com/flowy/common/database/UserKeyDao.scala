package com.flowy.common.database

import com.flowy.common.models.{ApiKeyStatus, Exchange, UserKey}
import java.util.UUID
import scala.concurrent.Future

trait UserKeyDao {
  def add(userKey: UserKey): Future[Option[UserKey]]

  def findAllWithStatus(status: ApiKeyStatus.Value): Future[Seq[UserKey]]

  def findById(keyId: UUID): Future[Option[UserKey]]

  def findByKeyPair(key: String, secret: String): Future[Option[UserKey]]

  def findByUserId(userId: UUID, keyId: UUID): Future[Option[UserKey]]

  def findByUserId(userId: UUID, status: ApiKeyStatus.Value = ApiKeyStatus.Verified): Future[Seq[UserKey]]

  def findByUserIdEx(userId: UUID, exchange: Exchange.Value): Future[Option[UserKey]]

  def findByUserIdAndKey(userId: UUID, key: String): Future[Option[UserKey]]

  def updateKey(ukey: UserKey): Future[Boolean]

  def remove(userId: UUID, exchange: Exchange.Value): Future[Boolean]
}

