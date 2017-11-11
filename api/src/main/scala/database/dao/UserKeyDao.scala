package database.dao

import java.util.UUID
import models.UserKey
import scala.concurrent.Future

trait UserKeyDao {
  def add(userKey: UserKey): Future[Unit]
  def findById(keyId: UUID): Future[Option[UserKey]]
  def findByKeyPair(key: String, secret: String): Future[Option[UserKey]]
  def findByUserId(userId: UUID, key: String): Future[Option[UserKey]]
  def remove(keyID: UUID): Future[Unit]
}

