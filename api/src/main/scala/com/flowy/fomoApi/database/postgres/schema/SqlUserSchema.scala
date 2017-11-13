package com.flowy.fomoApi.database.postgres.schema

import java.time.OffsetDateTime
import java.util.UUID
import com.flowy.marketmaker.common.sql.SqlDatabase
import com.flowy.marketmaker.models.{BasicUserData, User}

/**
  * The schemas are in separate traits, so that if your DAO would require to access (e.g. join) multiple tables,
  * you can just mix in the necessary traits and have the `TableQuery` definitions available.
  */
trait SqlUserSchema {

  protected val database: SqlDatabase

  import database._
  import database.driver.api._

  protected val users = TableQuery[Users]

  protected class Users(tag: Tag) extends Table[User](tag, "users") {
    // format: OFF
    def id              = column[UUID]("id", O.PrimaryKey)
    def first           = column[String]("first_name")
    def last            = column[String]("last_name")
    def email           = column[String]("email")
    def password        = column[String]("password")
    def salt            = column[String]("salt")
    def createdOn       = column[OffsetDateTime]("created_on")

    def * = (id, email, first, last, password, salt) <> ((User.apply _).tupled, User.unapply)
    def basic = (id, first, last, email) <> ((BasicUserData.apply _).tupled, BasicUserData.unapply)
    // format: ON
  }
}

