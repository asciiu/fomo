package com.softwaremill.bootzooka.passwordreset.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.softwaremill.bootzooka.passwordreset.application.{PasswordResetConfig, PasswordResetService}
import com.softwaremill.bootzooka.passwordreset.domain.PasswordResetCode
import com.softwaremill.bootzooka.test.{BaseRoutesSpec, TestHelpersWithDb}
import com.typesafe.config.ConfigFactory
import database.cassandra.CassandraPasswordResetCodeDao
import database.dao.PasswordResetCodeDao
import models.User
import routes.PasswordResetRoutes

class PasswordResetRoutesSpec extends BaseRoutesSpec with TestHelpersWithDb { spec =>

  lazy val config = new PasswordResetConfig {
    override def rootConfig = ConfigFactory.load()
  }
  val passwordResetCodeDao = new CassandraPasswordResetCodeDao(cqlDatabase)
  val passwordResetService =
    new PasswordResetService(userDao, passwordResetCodeDao, emailService, emailTemplatingEngine, config)

  val routes = Route.seal(new PasswordResetRoutes with TestRoutesSupport {
    override val userService          = spec.userService
    override val passwordResetService = spec.passwordResetService
  }.passwordResetRoutes)

  "POST /" should "send e-mail to user" in {
    // given
    val user = newRandomStoredUser()

    // when
    Post("/passwordreset", Map("email" -> user.email)) ~> routes ~> check {
      emailService.wasEmailSentTo(user.email) should be(true)
    }
  }

  "POST /[code] with password" should "change the password" in {
    // given
    val user = newRandomStoredUser()
    val code = PasswordResetCode(randomString(), user)
    passwordResetCodeDao.add(code).futureValue

    val newPassword = randomString()

    // when
    Post(s"/passwordreset/${code.code}", Map("password" -> newPassword)) ~> routes ~> check {
      responseAs[String] should be("ok")
      User.passwordsMatch(newPassword, userDao.findById(user.id).futureValue.get) should be(true)
    }
  }

  "POST /[code] without password" should "result in an error" in {
    // given
    val user = newRandomStoredUser()
    val code = PasswordResetCode(randomString(), user)
    passwordResetCodeDao.add(code).futureValue

    // when
    Post("/passwordreset/123") ~> routes ~> check {
      status should be(StatusCodes.BadRequest)
    }
  }

  "POST /[code] with password but with invalid code" should "result in an error" in {
    // given
    val user = newRandomStoredUser()
    val code = PasswordResetCode(randomString(), user)
    passwordResetCodeDao.add(code).futureValue

    val newPassword = randomString()

    // when
    Post("/passwordreset/123", Map("password" -> newPassword)) ~> routes ~> check {
      status should be(StatusCodes.Forbidden)
      User.passwordsMatch(newPassword, userDao.findById(user.id).futureValue.get) should be(false)
    }
  }
}
