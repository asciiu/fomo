package models

import java.time.temporal.ChronoUnit
import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

/**
  * Code used in the process of password reset.
  * By default this code has the `validTo` set to now plus 24 hours.
  */
case class PasswordResetCode(id: UUID, code: String, user: User, validTo: OffsetDateTime)

object PasswordResetCode {
  def apply(code: String, user: User): PasswordResetCode = {
    val nextDay = Instant.now().plus(24, ChronoUnit.HOURS).atOffset(ZoneOffset.UTC)
    PasswordResetCode(UUID.randomUUID(), code, user, nextDay)
  }
}
