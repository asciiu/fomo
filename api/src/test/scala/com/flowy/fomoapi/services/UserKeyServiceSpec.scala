package com.flowy.fomoapi.services

import java.util.UUID

import com.flowy.fomoapi.models.UserKey
import com.softwaremill.bootzooka.test.{FlatSpecWithDb, TestHelpersWithDb}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._

class UserKeyServiceSpec extends FlatSpecWithDb with Matchers with TestHelpersWithDb {

  override protected def beforeEach() = {
    super.beforeEach()
    userDao.add(newUser("Admin", "first", "apikeyuser@test.com", "pass", "salt")).futureValue
    userDao.add(newUser("Admin2", "second", "apikeyuser2@test.com", "pass", "salt")).futureValue
  }

  "UserKeyService" should "add a new user's key" in {

    val user = userDao.findByEmail("apikeyuser@test.com").futureValue
    val future = userKeyService.addUserKey(user.get.id, "test", "key", "secret", "this is a test key").futureValue

    future.isRight should be (true)
    future should matchPattern { case Right(_) => }
  }

  it should "not add a duplicate user key" in {

    val user = userDao.findByEmail("apikeyuser@test.com").futureValue
    for {
      _ <- userKeyService.addUserKey(user.get.id, "Testex", "key", "secret", "this is a test key")
      future2 <- userKeyService.addUserKey(user.get.id, "Testex", "key", "secret", "this is a test key")
    } yield {
      future2 should matchPattern { case Left("user key already exists") => }
    }
  }

  it should "retrieve a user key by user id" in {
    val key = "test_key"

    for {
      user <- userDao.findByEmail("apikeyuser2@test.com")
      _ <- userKeyService.addUserKey(user.get.id, "test", key, "secret", "this is a test key")
      future2 <- userKeyService.getUserKey(user.get.id, key)
    } yield {
      future2.isDefined should be(true)
      future2.get.key should be(key)
    }
  }

  it should "update a user key" in {
    val key = "test_key"
    val newKey = "new_key"
    val newSecret = "new_secret"
    val newDesc = "this is new"
    val userId = UUID.randomUUID()
    val wrongKey = UserKey.withRandomUUID(userId, "Testex", "test_key", "test_secret", "Update test")

    val user = userDao.findByEmail("apikeyuser2@test.com").futureValue
    val addedKey = userKeyService.addUserKey(user.get.id, "Testex", key, "secret", "this is a test key").futureValue match {
      case Right(k) => k
      case Left(msg) => wrongKey
    }

    val updated = userKeyService.update(addedKey.copy(
      key = newKey,
      secret = newSecret,
      description = newDesc)).futureValue

    val lekey = userKeyService.getUserKey(user.get.id, newKey).futureValue

    lekey.isDefined should be(true)
    val savedKey = lekey.get
    // you can't update the id or userId
    savedKey.id should be(addedKey.id)
    savedKey.userId should be(user.get.id)
    savedKey.exchange should be(addedKey.exchange)

    // these should be updated
    savedKey.key should be(newKey)
    savedKey.secret should be(newSecret)
    savedKey.description should be(newDesc)
  }
}
