package com.softwaremill.bootzooka.test

import com.flowy.common.database.postgres.SqlUserKeyDao
import com.softwaremill.bootzooka.email.application.{DummyEmailService, EmailTemplatingEngine}
import com.flowy.fomoapi.database.postgres.SqlUserDao
import com.flowy.fomoapi.services.{UserKeyService, UserService}
import com.flowy.common.utils.sql.SqlDatabase
import com.flowy.common.models.User
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext

trait TestHelpersWithDb extends TestHelpers with ScalaFutures {

  lazy val userDao               = new SqlUserDao(sqlDatabase)
  lazy val userKeyDao            = new SqlUserKeyDao(sqlDatabase)
  lazy val emailService          = new DummyEmailService()
  lazy val emailTemplatingEngine = new EmailTemplatingEngine
  lazy val userService           = new UserService(userDao, emailService, emailTemplatingEngine)
  lazy val userKeyService        = new UserKeyService(userKeyDao)

  def sqlDatabase: SqlDatabase

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def newRandomStoredUser(password: Option[String] = None): User = {
    val u = newRandomUser(password)
    userDao.add(u).futureValue
    u
  }
}
