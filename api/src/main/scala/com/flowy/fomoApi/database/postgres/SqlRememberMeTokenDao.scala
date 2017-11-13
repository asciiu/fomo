package com.flowy.fomoApi.database.postgres

import com.flowy.marketmaker.common.sql.SqlDatabase
import com.flowy.marketmaker.common.FutureHelpers._
import com.flowy.fomoApi.database.dao.RememberMeTokenDao
import com.flowy.fomoApi.database.postgres.schema.SqlRememberMeSchema
import com.flowy.fomoApi.models.RememberMeToken

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
