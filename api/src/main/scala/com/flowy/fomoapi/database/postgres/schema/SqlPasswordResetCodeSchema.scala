package com.flowy.fomoapi.database.postgres.schema

import com.flowy.marketmaker.common.sql.SqlDatabase
import java.time.OffsetDateTime
import java.util.UUID
import com.flowy.fomoapi.models.PasswordResetCode

trait SqlPasswordResetCodeSchema { this: SqlUserSchema =>

  protected val database: SqlDatabase

  import database._
  import database.driver.api._

  protected val passwordResetCodes = TableQuery[PasswordResetCodes]

  protected case class SqlPasswordResetCode(id: UUID, code: String, userId: UUID, validTo: OffsetDateTime)

  protected object SqlPasswordResetCode extends ((UUID, String, UUID, OffsetDateTime) => SqlPasswordResetCode) {
    def apply(rc: PasswordResetCode): SqlPasswordResetCode =
      SqlPasswordResetCode(rc.id, rc.code, rc.user.id, rc.validTo)
  }

  // format: OFF
  protected class PasswordResetCodes(tag: Tag) extends Table[SqlPasswordResetCode](tag, "password_reset_codes") {
    def id        = column[UUID]("id", O.PrimaryKey)
    def code      = column[String]("code")
    def userId    = column[UUID]("user_id")
    def validTo   = column[OffsetDateTime]("valid_to")

    def *         = (id, code, userId, validTo) <> (SqlPasswordResetCode.tupled, SqlPasswordResetCode.unapply)

    def user      = foreignKey("password_reset_code_user_fk", userId, users)(
      _.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)
    // format: ON
  }

}

