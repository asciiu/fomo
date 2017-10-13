package com.softwaremill.bootzooka.test

import java.time.{OffsetDateTime, ZoneOffset}

import models.User

trait TestHelpers {

  val createdOn = OffsetDateTime.of(2015, 6, 3, 13, 25, 3, 0, ZoneOffset.UTC)

  private val random     = new scala.util.Random
  private val characters = "abcdefghijklmnopqrstuvwxyz0123456789"

  def randomString(length: Int = 10) =
    Stream.continually(random.nextInt(characters.length)).map(characters).take(length).mkString

  def newUser(first: String, last: String, email: String, pass: String, salt: String): User =
    User.withRandomUUID(first, last, email, pass, salt)

  def newRandomUser(password: Option[String] = None): User = {
    val first = randomString()
    val last = randomString()
    val pass  = password.getOrElse(randomString())
    newUser(first, last, s"$first.$last@example.com", pass, "someSalt")
  }
}
