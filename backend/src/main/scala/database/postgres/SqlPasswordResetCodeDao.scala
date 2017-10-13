package database.postgres

import java.time.OffsetDateTime
import java.util.UUID

import com.softwaremill.bootzooka.common.FutureHelpers._
import com.softwaremill.bootzooka.common.sql.SqlDatabase
import database.dao.PasswordResetCodeDao
import database.postgres.schema.{SqlPasswordResetCodeSchema, SqlUserSchema}
import models.{PasswordResetCode, User}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions


class SqlPasswordResetCodeDao(protected val database: SqlDatabase)(implicit ec: ExecutionContext)
  extends PasswordResetCodeDao with SqlPasswordResetCodeSchema with SqlUserSchema {

  import database._
  import database.driver.api._

  def add(code: PasswordResetCode): Future[Unit] =
    db.run(passwordResetCodes += SqlPasswordResetCode(code)).mapToUnit

  def findByCode(code: String): Future[Option[PasswordResetCode]] = findFirstMatching(_.code === code)

  private def findFirstMatching(condition: PasswordResetCodes => Rep[Boolean]): Future[Option[PasswordResetCode]] = {
    val q = for {
      resetCode <- passwordResetCodes.filter(condition)
      user      <- resetCode.user
    } yield (resetCode, user)

    val conversion: PartialFunction[(SqlPasswordResetCode, User), PasswordResetCode] = {
      case (rc, u) => PasswordResetCode(rc.id, rc.code, u, rc.validTo)
    }

    db.run(convertFirstResultItem(q.result.headOption, conversion))
  }

  def remove(code: PasswordResetCode): Future[Unit] =
    db.run(passwordResetCodes.filter(_.id === code.id).delete).mapToUnit

  private def convertFirstResultItem[A, B](action: DBIOAction[Option[A], _, _], conversion: (PartialFunction[A, B])) =
    action.map(_.map(conversion))
}
