package com.flowy.common

import java.time.{OffsetDateTime, ZoneOffset}
import java.time.temporal.ChronoUnit

object Util {
  def currentTimeRoundedDown(minutes: Int) = {
    roundDateToMinute(now(), minutes)
  }

  def now() = OffsetDateTime.now(ZoneOffset.UTC)

  def roundDateToMinute(dateTime: OffsetDateTime, minutes: Int): OffsetDateTime = {
    if (minutes < 1 || 5 % minutes != 0) {
      throw new IllegalArgumentException("minutes must be a factor of 5")
    }

    val m = dateTime.getMinute() / minutes
    dateTime.withMinute(minutes* m).truncatedTo(ChronoUnit.MINUTES)
  }

  def roundUpPrecision4(number: Double): BigDecimal = {
    BigDecimal(number).setScale(4, BigDecimal.RoundingMode.CEILING)
  }

  def roundDownPrecision4(number: Double): BigDecimal = {
    BigDecimal(number).setScale(4, BigDecimal.RoundingMode.FLOOR)
  }

  def roundUpPrecision8(number: Double): BigDecimal = {
    BigDecimal(number).setScale(8, BigDecimal.RoundingMode.CEILING)
  }
}
