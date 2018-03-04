package com.flowy.common.database.postgres.schema

import java.time.OffsetDateTime
import java.util.UUID

import com.flowy.common.models.{ApiKeyStatus, Exchange, UserKey}
import com.flowy.common.utils.sql.SqlDatabase

trait SqlUserKeySchema {
  protected val database: SqlDatabase

  import database._
  import database.driver.api._

  protected val userKeys = TableQuery[UserKeys]

  implicit val exchangeStatusMapper = MappedColumnType.base[Exchange.Value, String](
    { os => os.toString }, { str => Exchange.withName(str) }
  )
  implicit val apiKeyStatusMapper = MappedColumnType.base[ApiKeyStatus.Value, String](
    { os => os.toString }, { str => ApiKeyStatus.withName(str) }
  )

  protected class UserKeys(tag: Tag) extends Table[UserKey](tag, "user_api_keys") {
    // format: OFF
    def id              = column[UUID]("id", O.PrimaryKey)
    def userId          = column[UUID]("user_id")
    def exchange        = column[Exchange.Value]("exchange_name")
    def key             = column[String]("api_key")
    def secret          = column[String]("secret")
    def description     = column[String]("description")
    def status          = column[ApiKeyStatus.Value]("status")
    def createdOn       = column[OffsetDateTime]("created_on")
    def updatedOn       = column[OffsetDateTime]("updated_on")

    def * = (id, userId, exchange, key, secret, description, status, createdOn, updatedOn) <>
      ((UserKey.apply _).tupled, UserKey.unapply)

    // format: ON
  }
}
