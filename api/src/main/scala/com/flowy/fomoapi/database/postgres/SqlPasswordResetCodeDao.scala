package com.flowy.fomoapi.database.postgres

import com.flowy.common.utils.sql.SqlDatabase
import com.flowy.common.utils.FutureHelpers._
import com.flowy.fomoapi.database.dao.PasswordResetCodeDao
import com.flowy.fomoapi.database.postgres.schema.{SqlPasswordResetCodeSchema, SqlUserSchema}
import com.flowy.fomoapi.models.PasswordResetCode
import com.flowy.common.models.User

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
