package com.softwaremill.bootzooka.user.application

import org.scalatest.{FlatSpec, Matchers}
import services.UserRegisterValidator

class UserRegisterValidatorSpec extends FlatSpec with Matchers {

  "validate" should "accept valid data" in {
    val dataIsValid = UserRegisterValidator.validate("admin@sml.com", "password")

    dataIsValid should be(Right(()))
  }

  "validate" should "not accept missing email with spaces only" in {
    val dataIsValid = UserRegisterValidator.validate("   ", "password")

    dataIsValid should be('left)
  }

  "validate" should "not accept invalid email" in {
    val dataIsValid = UserRegisterValidator.validate("invalidEmail", "password")

    dataIsValid should be('left)
  }

  "validate" should "not accept password with empty spaces only" in {
    val dataIsValid = UserRegisterValidator.validate("admin@sml.com", "    ")

    dataIsValid should be('left)
  }
}
