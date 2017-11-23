package services

import java.util.UUID

import com.softwaremill.bootzooka.test.{FlatSpecWithDb, TestHelpersWithDb}
import com.flowy.fomoapi.services.UserRegisterResult
import com.flowy.marketmaker.models.User
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._

class UserServiceSpec extends FlatSpecWithDb with Matchers with TestHelpersWithDb {

  override protected def beforeEach() = {
    super.beforeEach()

    userDao.add(newUser("Admin", "first", "admin@sml.com", "pass", "salt")).futureValue
    userDao.add(newUser("Admin2", "second", "admin2@sml.com", "pass", "salt")).futureValue
  }

  "registerNewUser" should "add user with unique lowercase login info" in {
    // When
    val result = userService.registerNewUser("John", "Doe", "newUser@sml.com", "password").futureValue

    // Then
    result should be(UserRegisterResult.Success)

    val userOpt = userDao.findByEmail("newUser@sml.com").futureValue
    userOpt should be('defined)
    val user = userOpt.get

    user.firstName should be("John")
    user.lastName should be("Doe")

    emailService.wasEmailSentTo("newUser@sml.com") should be(true)
  }

  "registerNewUser" should "not register a user if a user with the given login/e-mail exists" in {
    // when
    val resultInitial   = userService.registerNewUser("John", "Doe", "newUser@sml.com", "password").futureValue
    val resultSameLogin = userService.registerNewUser("John", "Doe", "newUser2@sml.com", "password").futureValue
    val resultSameEmail = userService.registerNewUser("John2", "Doe", "newUser@sml.com", "password").futureValue

    // then
    resultInitial should be(UserRegisterResult.Success)
    resultSameLogin should matchPattern { case UserRegisterResult.Success => }
    resultSameEmail should matchPattern { case UserRegisterResult.UserExists(_) => }
  }

  "registerNewUser" should "not schedule an email on existing login" in {
    // When
    userService.registerNewUser("Admin", "One", "admin3@sml.com", "password").futureValue

    // Then
    emailService.wasEmailSentTo("admin@sml.com") should be(false)
  }

  "changeEmail" should "change email for specified user" in {
    val userFuture    = userDao.findByEmail("admin@sml.com")
    val userValue     = Await.result(userFuture, 5 seconds)
    val newEmail = "new@email.com"
    userService.changeEmail(userValue.get.id, newEmail).futureValue should be('right)
    userDao.findByEmail(newEmail).futureValue match {
      case Some(cu) => // ok
      case None     => fail("User not found. Maybe e-mail wasn't really changed?")
    }
  }

  "changeEmail" should "not change email if already used by someone else" in {
    userService.changeEmail(UUID.randomUUID(), "admin2@sml.com").futureValue should be('left)
  }

  "changePassword" should "change password if current is correct and new is present" in {
    // Given
    val user            = userDao.findByEmail("admin@sml.com").futureValue.get
    val currentPassword = "pass"
    val newPassword     = "newPass"

    // When
    val changePassResult = userService.changePassword(user.id, currentPassword, newPassword).futureValue

    // Then
    changePassResult should be('right)
    userDao.findByEmail("admin@sml.com").futureValue match {
      case Some(cu) => cu.passwordHash should be(User.encryptPassword(newPassword, cu.salt))
      case None     => fail("Something bad happened, maybe mocked Dao is broken?")
    }
  }

  "changePassword" should "not change password if current is incorrect" in {
    // Given
    val user = userDao.findByEmail("admin@sml.com").futureValue.get

    // When, Then
    userService.changePassword(user.id, "someillegalpass", "newpass").futureValue should be('left)
  }

  "changePassword" should "complain when user cannot be found" in {
    userService.changePassword(UUID.randomUUID(), "pass", "newpass").futureValue should be('left)
  }

}
