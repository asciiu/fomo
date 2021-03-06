package com.flowy.fomoapi.database.dao

import com.flowy.fomoapi.models.RememberMeToken

import scala.concurrent.Future


trait RememberMeTokenDao {
  def findBySelector(selector: String): Future[Option[RememberMeToken]]
  def add(data: RememberMeToken): Future[Unit]
  def remove(selector: String): Future[Unit]
}

//class RememberMeTokenDao(protected val com.flowy.fomoApi.database: SqlDatabase)(implicit ec: ExecutionContext)
//    extends SqlRememberMeSchema {
//
//  import com.flowy.fomoApi.database._
//  import com.flowy.fomoApi.database.driver.api._
//
//  def findBySelector(selector: String): Future[Option[RememberMeToken]] =
//    db.run(rememberMeTokens.filter(_.selector === selector).result).map(_.headOption)
//
//  def add(data: RememberMeToken): Future[Unit] =
//    db.run(rememberMeTokens += data).mapToUnit
//
//  def remove(selector: String): Future[Unit] =
//    db.run(rememberMeTokens.filter(_.selector === selector).delete).mapToUnit
//}
//
//trait SqlRememberMeSchema {
//  protected val com.flowy.fomoApi.database: SqlDatabase
//
//  import com.flowy.fomoApi.database._
//  import com.flowy.fomoApi.database.driver.api._
//
//  protected val rememberMeTokens = TableQuery[RememberMeTokens]
//
//  protected class RememberMeTokens(tag: Tag) extends Table[RememberMeToken](tag, "remember_me_tokens") {
//    def id        = column[UUID]("id", O.PrimaryKey)
//    def selector  = column[String]("selector")
//    def tokenHash = column[String]("token_hash")
//    def userId    = column[UUID]("user_id")
//    def validTo   = column[OffsetDateTime]("valid_to")
//
//    def * = (id, selector, tokenHash, userId, validTo) <> (RememberMeToken.tupled, RememberMeToken.unapply)
//  }
//}
