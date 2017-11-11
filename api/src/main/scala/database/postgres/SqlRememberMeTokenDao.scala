package database.postgres

import com.softwaremill.bootzooka.common.FutureHelpers._
import com.softwaremill.bootzooka.common.sql.SqlDatabase
import database.dao.RememberMeTokenDao
import database.postgres.schema.SqlRememberMeSchema
import models.RememberMeToken

import scala.concurrent.{ExecutionContext, Future}

class SqlRememberMeTokenDao(protected val database: SqlDatabase)(implicit ec: ExecutionContext)
  extends RememberMeTokenDao with SqlRememberMeSchema {

  import database._
  import database.driver.api._

  def findBySelector(selector: String): Future[Option[RememberMeToken]] =
    db.run(rememberMeTokens.filter(_.selector === selector).result).map(_.headOption)

  def add(data: RememberMeToken): Future[Unit] =
    db.run(rememberMeTokens += data).mapToUnit

  def remove(selector: String): Future[Unit] =
    db.run(rememberMeTokens.filter(_.selector === selector).delete).mapToUnit
}
