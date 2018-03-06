package com.softwaremill.bootzooka.passwordreset.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.passwordreset.application.PasswordResetConfig
import com.softwaremill.bootzooka.test.{BaseRoutesSpec, TestHelpersWithDb}
import com.typesafe.config.ConfigFactory
import com.flowy.fomoapi.database.postgres.SqlPasswordResetCodeDao
import com.flowy.fomoapi.models.PasswordResetCode
import com.flowy.fomoapi.routes.PasswordResetRoutes
import com.flowy.fomoapi.services.PasswordResetService
import com.flowy.common.models.User
import com.flowy.common.utils.sql.{DatabaseConfig, SqlDatabase}

class PasswordResetRoutesSpec extends BaseRoutesSpec with TestHelpersWithDb with RoutesSupport { spec =>

  //val sqlDatabase = SqlDatabase.create(config)
  lazy val conf = new PasswordResetConfig {
    override def rootConfig = ConfigFactory.load()
  }

  val passwordResetCodeDao = new SqlPasswordResetCodeDao(sqlDatabase)
  val passwordResetService =
    new PasswordResetService(userDao, passwordResetCodeDao, emailService, emailTemplatingEngine, conf)

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
      val reponse = responseAs[JSendResponse]
      reponse.status should be(JsonStatus.Success)
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
