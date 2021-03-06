package com.flowy.fomoapi.database.dao

import com.flowy.fomoapi.models.PasswordResetCode

import scala.concurrent.Future
import scala.language.implicitConversions

trait PasswordResetCodeDao {
  def add(code: PasswordResetCode): Future[Unit]
  def findByCode(code: String): Future[Option[PasswordResetCode]]
  def remove(code: PasswordResetCode): Future[Unit]
}
