package com.flowy.common.database.postgres

import java.time.OffsetDateTime
import java.util.UUID

import com.flowy.common.database.UserKeyDao
import com.flowy.common.database.postgres.schema.SqlUserKeySchema
import com.flowy.common.models.{ApiKeyStatus, Exchange, UserKey}
import com.flowy.common.utils.sql.SqlDatabase

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class SqlUserKeyDao (protected val database: SqlDatabase)(implicit val ec: ExecutionContext)
  extends UserKeyDao with SqlUserKeySchema {

  import database._
  import database.driver.api._

  private def findOneWhere(condition: UserKeys => Rep[Boolean]) =
    db.run(userKeys.filter(condition).result.headOption)

  def add(key: UserKey): Future[Option[UserKey]] = {
    val query = userKeys += key

    db.run(query.asTry).map { result =>
      result match {
        case Success(count) if count > 0 => Some(key)
        case _ => None
      }
    }
  }

  def findAllWithStatus(status: ApiKeyStatus.Value): Future[Seq[UserKey]] = {
    val query = userKeys.filter(_.status === status)
    db.run(query.result)
  }

  def findById(keyId: UUID): Future[Option[UserKey]] =
    findOneWhere(_.id === keyId)

  def findByKeyPair(key: String, secret: String): Future[Option[UserKey]] =
    findOneWhere(r => r.key === key && r.secret === secret)

  def findByUserId(userId: UUID, status: ApiKeyStatus.Value = ApiKeyStatus.Verified): Future[Seq[UserKey]] = {
    val query = userKeys.filter(k => k.userId === userId && k.status === status)
    db.run(query.result)
  }

  def findByUserIdEx(userId: UUID, exchange: Exchange.Value): Future[Option[UserKey]] =
    findOneWhere(r => r.userId === userId && r.exchange === exchange)

  def findByUserIdAndKey(userId: UUID, key: String): Future[Option[UserKey]] =
    findOneWhere(r => r.userId === userId && r.key === key)

  def remove(userId: UUID, exchange: Exchange.Value): Future[Boolean] = {
    val query = userKeys.filter(r => r.userId === userId && r.exchange === exchange).delete
    db.run( query.asTry ).map {result =>
      result match {
        case Success(count) if count > 0 => true
        case _ => false
      }
    }
  }

  def updateKey(ukey: UserKey): Future[Boolean] = {
    val updateAction = userKeys.filter(k => k.id === ukey.id && k.userId === ukey.userId && k.exchange === ukey.exchange)
          .map(lk => (lk.key, lk.secret, lk.description)).update((ukey.key, ukey.secret, ukey.description))

    db.run( updateAction.asTry ).map {result =>
      result match {
        case Success(count) if count > 0 => true
        case _ => false
      }
    }
  }
}