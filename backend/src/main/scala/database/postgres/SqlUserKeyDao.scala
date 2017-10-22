package database.postgres

import java.util.UUID

import com.softwaremill.bootzooka.common.FutureHelpers._
import com.softwaremill.bootzooka.common.sql.SqlDatabase
import database.dao.UserKeyDao
import database.postgres.schema.SqlUserKeySchema
import models.UserKey

import scala.concurrent.{ExecutionContext, Future}

class SqlUserKeyDao (protected val database: SqlDatabase)(implicit val ec: ExecutionContext)
  extends UserKeyDao with SqlUserKeySchema {

  import database._
  import database.driver.api._

  private def findOneWhere(condition: UserKeys => Rep[Boolean]) =
    db.run(userKeys.filter(condition).result.headOption)

  def add(key: UserKey): Future[Unit] =
    db.run(userKeys += key).mapToUnit

  def findById(keyId: UUID): Future[Option[UserKey]] =
    findOneWhere(_.id === keyId)

  def findByKeyPair(key: String, secret: String): Future[Option[UserKey]] =
    findOneWhere(r => r.key === key && r.secret === secret)

  def findByUserId(userId: UUID, key: String): Future[Option[UserKey]] =
    findOneWhere(r => r.userId === userId && r.key === key)

  def remove(keyId: UUID): Future[Unit] =
    db.run(userKeys.filter(_.id === keyId).delete).mapToUnit
}