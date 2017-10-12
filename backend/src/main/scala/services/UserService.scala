package services

import java.util.UUID

import com.softwaremill.bootzooka.common.Utils
import com.softwaremill.bootzooka.email.application.{EmailService, EmailTemplatingEngine}
import com.softwaremill.bootzooka.user._
import database.UserDao
import models.{BasicUserData, User}
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

class UserService(
    userDao: UserDao,
    emailService: EmailService,
    emailTemplatingEngine: EmailTemplatingEngine
)(implicit ec: ExecutionContext) {

  def findById(userId: UserId): Future[Option[BasicUserData]] =
    userDao.findBasicDataById(userId)

  def registerNewUser(first: String, last: String, email: String, password: String): Future[UserRegisterResult] = {
    def checkUserExistence(): Future[Either[String, Unit]] = {
      val existingEmailFuture = userDao.findByEmail(email)

      existingEmailFuture.map{ existingEmailOpt =>
        existingEmailOpt match {
          case Some(u) => Left("E-mail already in use!")
          case None => Right(():Unit)
        }
      }
    }

    def registerValidData() = checkUserExistence().flatMap {
      case Left(msg) => Future.successful(UserRegisterResult.UserExists(msg))
      case Right(_) =>
        val salt          = Utils.randomString(128)
        val userAddResult = userDao.add(
          User.withRandomUUID(email.toLowerCase, first, last, password, salt))
        userAddResult.foreach { _ =>
          val confirmationEmail = emailTemplatingEngine.registrationConfirmation(first)
          emailService.scheduleEmail(email, confirmationEmail)
        }
        userAddResult.map(_ => UserRegisterResult.Success)
    }

    UserRegisterValidator
      .validate(email, password)
      .fold(
        msg => Future.successful(UserRegisterResult.InvalidData(msg)),
        _ => registerValidData()
      )
  }

  def authenticate(email: String, nonEncryptedPassword: String): Future[Option[BasicUserData]] =
    userDao
      .findByEmail(email)
      .map(userOpt => userOpt.filter(u => User.passwordsMatch(nonEncryptedPassword, u)).map(BasicUserData.fromUser))

  def changeLogin(userId: UUID, newLogin: String): Future[Either[String, Unit]] =
    userDao.findByLowerCasedLogin(newLogin).flatMap {
      case Some(_) => Future.successful(Left("Login is already taken"))
      case None    => userDao.changeLogin(userId, newLogin).map(Right(_))
    }

  def changeEmail(userId: UUID, newEmail: String): Future[Either[String, Unit]] =
    userDao.findByEmail(newEmail).flatMap {
      case Some(_) => Future.successful(Left("E-mail used by another user"))
      case None    => userDao.changeEmail(userId, newEmail).map(Right(_))
    }

  def changePassword(userId: UUID, currentPassword: String, newPassword: String): Future[Either[String, Unit]] =
    userDao.findById(userId).flatMap {
      case Some(u) =>
        if (User.passwordsMatch(currentPassword, u)) {
          userDao.changePassword(u.id, User.encryptPassword(newPassword, u.salt)).map(Right(_))
        } else Future.successful(Left("Current password is invalid"))

      case None => Future.successful(Left("User not found hence cannot change password"))
    }
}

sealed trait UserRegisterResult

object UserRegisterResult {

  case class InvalidData(msg: String) extends UserRegisterResult

  case class UserExists(msg: String) extends UserRegisterResult

  case object Success extends UserRegisterResult

}

object UserRegisterValidator {
  private val ValidationOk = Right(())
  val MinLoginLength       = 3

  def validate(email: String, password: String): Either[String, Unit] =
    for {
      _ <- validEmail(email.trim).right
      _ <- validPassword(password.trim).right
    } yield ()

  private def validLogin(login: String) =
    if (login.length >= MinLoginLength) ValidationOk else Left("Login is too short!")

  private val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  private def validEmail(email: String) =
    if (emailRegex.findFirstMatchIn(email).isDefined) ValidationOk else Left("Invalid e-mail!")

  private def validPassword(password: String) =
    if (password.nonEmpty) ValidationOk else Left("Password cannot be empty!")
}
