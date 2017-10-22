package database.postgres

import com.softwaremill.bootzooka.common.FutureHelpers._
import com.softwaremill.bootzooka.common.sql.SqlDatabase
import com.softwaremill.bootzooka.user._
import database.dao.UserDao
import database.postgres.schema.SqlUserSchema
import models.{BasicUserData, User}

import scala.concurrent.{ExecutionContext, Future}

class SqlUserDao(protected val database: SqlDatabase)(implicit val ec: ExecutionContext)
  extends UserDao with SqlUserSchema {

  import database._
  import database.driver.api._

  private def findOneWhere(condition: Users => Rep[Boolean]) = db.run(users.filter(condition).result.headOption)

  def add(user: User): Future[Unit] = db.run(users += user).mapToUnit

  def findById(userId: UserId): Future[Option[User]] = findOneWhere(_.id === userId)

  def findBasicDataById(userId: UserId): Future[Option[BasicUserData]] =
    db.run(users.filter(_.id === userId).map(_.basic).result.headOption)

  def findByEmail(email: String): Future[Option[User]] = findOneWhere(_.email.toLowerCase === email.toLowerCase)

  def changePassword(userId: UserId, newPassword: String): Future[Unit] =
    db.run(users.filter(_.id === userId).map(_.password).update(newPassword)).mapToUnit

  def changeEmail(userId: UserId, newEmail: String): Future[Unit] =
    db.run(users.filter(_.id === userId).map(_.email).update(newEmail)).mapToUnit
}