package com.softwaremill.bootzooka.test

import com.softwaremill.bootzooka.common.sql.SqlDatabase
import com.softwaremill.bootzooka.email.application.{DummyEmailService, EmailTemplatingEngine}
import database.postgres.{SqlUserDao, SqlUserKeyDao}
import models.User
import org.scalatest.concurrent.ScalaFutures
import services.{UserKeyService, UserService}

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
