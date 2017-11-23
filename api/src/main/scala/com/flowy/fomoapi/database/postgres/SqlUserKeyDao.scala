package com.flowy.fomoapi.database.postgres

import java.util.UUID

import com.flowy.marketmaker.common.sql.SqlDatabase
import com.flowy.marketmaker.common.FutureHelpers._
import com.flowy.fomoapi.database.dao.UserKeyDao
import com.flowy.fomoapi.database.postgres.schema.SqlUserKeySchema
import com.flowy.fomoapi.models.UserKey

import scala.concurrent.{ExecutionContext, Future}

class SqlUserKeyDao (protected val database: SqlDatabase)(implicit val ec: ExecutionContext)
  extends UserKeyDao with SqlUserKeySchema {

  import database._
  import database.driver.api._

  private def findOneWhere(condition: UserKeys => Rep[Boolean]) =
    db.run(userKeys.filter(condition).result.headOption)

  def add(key: UserKey): Future[UUID] =
    db.run(userKeys returning userKeys.map(_.id) += key)

  def findById(keyId: UUID): Future[Option[UserKey]] =
    findOneWhere(_.id === keyId)

  def findByKeyPair(key: String, secret: String): Future[Option[UserKey]] =
    findOneWhere(r => r.key === key && r.secret === secret)

  def findByUserId(userId: UUID, exchange: String): Future[Seq[UserKey]] =
    db.run(userKeys.filter(r => r.userId === userId && r.exchange === exchange).result)

  def findByUserIdAndKey(userId: UUID, key: String): Future[Option[UserKey]] =
    findOneWhere(r => r.userId === userId && r.key === key)

  def remove(keyId: UUID): Future[Unit] =
    db.run(userKeys.filter(_.id === keyId).delete).mapToUnit

  def upsert(ukey: UserKey): Future[Boolean] = {
    db.run(userKeys.insertOrUpdate(ukey)).foreach(println)
    Future.successful(true)
  }
}
