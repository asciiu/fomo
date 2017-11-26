package com.flowy.common.database.postgres.schema

import java.time.OffsetDateTime
import java.util.UUID

import com.flowy.common.models.UserKey
import com.flowy.common.utils.sql.SqlDatabase

trait SqlUserKeySchema {
  protected val database: SqlDatabase

  import database._
  import database.driver.api._

  protected val userKeys = TableQuery[UserKeys]

  protected class UserKeys(tag: Tag) extends Table[UserKey](tag, "user_api_keys") {
    // format: OFF
    def id              = column[UUID]("id", O.PrimaryKey)
    def userId          = column[UUID]("user_id")
    def exchange        = column[String]("exchange")
    def key             = column[String]("api_key")
    def secret          = column[String]("secret")
    def description     = column[String]("description")
    def createdOn       = column[OffsetDateTime]("created_on")
    def updatedOn       = column[OffsetDateTime]("updated_on")

    def * = (id, userId, exchange, key, secret, description, createdOn, updatedOn) <>
      ((UserKey.apply _).tupled, UserKey.unapply)

    // format: ON
  }
}
