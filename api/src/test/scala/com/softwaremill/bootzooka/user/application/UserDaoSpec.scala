package com.softwaremill.bootzooka.user.application

import java.util.UUID

import com.softwaremill.bootzooka.test.{FlatSpecWithDb, TestHelpers}
import com.typesafe.scalalogging.StrictLogging
import database.postgres.SqlUserDao
import models.User
import org.scalatest.Matchers

import scala.language.implicitConversions

class UserDaoSpec extends FlatSpecWithDb with StrictLogging with TestHelpers with Matchers {
  behavior of "UserDao"

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  val userDao        = new SqlUserDao(sqlDatabase)
  lazy val randomIds = List.fill(3)(UUID.randomUUID())

  override def beforeEach() {
    super.beforeEach()
    for (i <- 1 to randomIds.size) {
      val first    = "first" + i
      val last     = "last" + i
      val password = "pass" + i
      val salt     = "salt" + i
      userDao
        .add(User(randomIds(i - 1), i + "email@sml.com", first, last, password, salt))
        .futureValue
    }
  }

  it should "add new user" in {
    // Given
    val first = "test1"
    val last  = "test2"
    val login = "newuser"
    val email = "newemail@sml.com"

    // When
    userDao.add(newUser(first, last, email, "pass", "salt")).futureValue

    // Then
    userDao.findByEmail(email).futureValue should be('defined)
  }

  it should "fail with exception when trying to add user with existing email" in {
    // Given
    val first = "test1"
    val last  = "test2"
    val email = "anotherEmaill@sml.com"

    userDao.add(newUser(first, last, email, "somePass", "someSalt")).futureValue

    // When & then
    userDao.add(newUser(first, last, email, "pass", "salt")).failed.futureValue
  }

  it should "find by email" in {
    // Given
    val email = "1email@sml.com"

    // When
    val userOpt = userDao.findByEmail(email).futureValue

    // Then
    userOpt.map(_.email) should equal(Some(email))
  }

  it should "find by uppercase email" in {
    // Given
    val email = "1email@sml.com".toUpperCase

    // When
    val userOpt = userDao.findByEmail(email).futureValue

    // Then
    userOpt.map(_.email) should equal(Some(email.toLowerCase))
  }

  it should "change password" in {
    // Given
    val email    = "1email@sml.com"
    val password = User.encryptPassword("pass11", "salt1")
    val user     = userDao.findByEmail(email).futureValue.get

    // When
    userDao.changePassword(user.id, password).futureValue
    val postModifyUserOpt = userDao.findByEmail(email).futureValue
    val u                 = postModifyUserOpt.get

    // Then
    u should be(user.copy(passwordHash = password))
  }

  it should "change email" in {
    // Given
    val newEmail = "newmail@sml.pl"
    val user     = userDao.findByEmail("1email@sml.com").futureValue
    val u        = user.get

    // When
    userDao.changeEmail(u.id, newEmail).futureValue

    // Then
    userDao.findByEmail(newEmail).futureValue should equal(Some(u.copy(email = newEmail)))
  }
}
