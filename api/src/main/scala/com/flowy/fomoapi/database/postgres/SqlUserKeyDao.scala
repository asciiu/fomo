package com.flowy.fomoapi.database.postgres

import java.util.UUID

import com.flowy.marketmaker.common.sql.SqlDatabase
import com.flowy.fomoapi.database.dao.UserKeyDao
import com.flowy.fomoapi.database.postgres.schema.SqlUserKeySchema
import com.flowy.fomoapi.models.UserKey

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

  def findById(keyId: UUID): Future[Option[UserKey]] =
    findOneWhere(_.id === keyId)

  def findByKeyPair(key: String, secret: String): Future[Option[UserKey]] =
    findOneWhere(r => r.key === key && r.secret === secret)

  def findByUserId(userId: UUID, exchange: String): Future[Option[UserKey]] =
    findOneWhere(r => r.userId === userId && r.exchange === exchange)

  def findByUserIdAndKey(userId: UUID, key: String): Future[Option[UserKey]] =
    findOneWhere(r => r.userId === userId && r.key === key)

  def remove(userId: UUID, exchange: String): Future[Boolean] = {
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
