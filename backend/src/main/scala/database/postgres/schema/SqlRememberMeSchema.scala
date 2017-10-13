package database.postgres.schema

import com.softwaremill.bootzooka.common.sql.SqlDatabase
import java.time.OffsetDateTime
import java.util.UUID
import models.RememberMeToken

trait SqlRememberMeSchema {
  protected val database: SqlDatabase

  import database._
  import database.driver.api._

  protected val rememberMeTokens = TableQuery[RememberMeTokens]

  protected class RememberMeTokens(tag: Tag) extends Table[RememberMeToken](tag, "remember_me_tokens") {
    def id        = column[UUID]("id", O.PrimaryKey)
    def selector  = column[String]("selector")
    def tokenHash = column[String]("token_hash")
    def userId    = column[UUID]("user_id")
    def validTo   = column[OffsetDateTime]("valid_to")

    def * = (id, selector, tokenHash, userId, validTo) <> (RememberMeToken.tupled, RememberMeToken.unapply)
  }
}

