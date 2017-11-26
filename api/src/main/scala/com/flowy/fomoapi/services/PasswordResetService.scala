package com.flowy.fomoapi.services

import java.time.Instant

import com.flowy.fomoapi.models.PasswordResetCode
import com.flowy.common.utils.Utils
import com.softwaremill.bootzooka.email.application.{EmailService, EmailTemplatingEngine}
import com.softwaremill.bootzooka.email.domain.EmailContentWithSubject
import com.softwaremill.bootzooka.passwordreset.application.PasswordResetConfig
import com.typesafe.scalalogging.StrictLogging
import com.flowy.fomoapi.database.dao.{PasswordResetCodeDao, UserDao}
import com.flowy.common.models.User

import scala.concurrent.{ExecutionContext, Future}

class PasswordResetService(
    userDao: UserDao,
    codeDao: PasswordResetCodeDao,
    emailService: EmailService,
    emailTemplatingEngine: EmailTemplatingEngine,
    config: PasswordResetConfig
)(implicit ec: ExecutionContext)
    extends StrictLogging {

  def sendResetCodeToUser(email: String): Future[Unit] = {
    logger.debug(s"Preparing to generate and send reset code to user $email")
    val userFut = userDao.findByEmail(email)
    userFut.flatMap {
      case Some(user) =>
        logger.debug("User found")
        val code = randomPass(user)
        storeCode(code).flatMap(_ => sendCode(code))
      case None =>
        logger.debug(s"User not found: $email")
        Future.successful((): Unit)
    }
  }

  private def randomPass(user: User): PasswordResetCode = PasswordResetCode(Utils.randomString(32), user)

  private def storeCode(code: PasswordResetCode): Future[Unit] = {
    logger.debug(s"Storing reset code for user ${code.user.email}")
    codeDao.add(code)
  }

  private def sendCode(code: PasswordResetCode): Future[Unit] = {
    logger.debug(s"Scheduling e-mail with reset code for user ${code.user.email}")
    emailService.scheduleEmail(code.user.email, prepareResetEmail(code.user, code))
  }

  private def prepareResetEmail(user: User, code: PasswordResetCode): EmailContentWithSubject = {
    val resetLink = String.format(config.resetLinkPattern, code.code)
    emailTemplatingEngine.passwordReset(user.email, resetLink)
  }

  def performPasswordReset(code: String, newPassword: String): Future[Either[String, Boolean]] = {
    logger.debug("Performing password reset")
    codeDao.findByCode(code).flatMap {
      case Some(c) =>
        if (c.validTo.toInstant.isAfter(Instant.now())) {
          for {
            _ <- changePassword(c, newPassword)
            _ <- invalidateResetCode(c)
          } yield Right(true)
        } else {
          invalidateResetCode(c).map(_ => Left("Your reset code is invalid. Please try again."))
        }
      case None =>
        logger.debug("Reset code not found")
        Future.successful(Left("Your reset code is invalid. Please try again."))
    }
  }

  private def changePassword(code: PasswordResetCode, newPassword: String): Future[Unit] =
    userDao.changePassword(code.user.id, User.encryptPassword(newPassword, code.user.salt))

  private def invalidateResetCode(code: PasswordResetCode): Future[Unit] =
    codeDao.remove(code)
}
