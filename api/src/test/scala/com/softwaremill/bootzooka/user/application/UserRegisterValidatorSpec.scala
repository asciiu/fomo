package com.softwaremill.bootzooka.user.application

import com.flowy.fomoapi.services.UserRegisterValidator
import org.scalatest.{FlatSpec, Matchers}

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
