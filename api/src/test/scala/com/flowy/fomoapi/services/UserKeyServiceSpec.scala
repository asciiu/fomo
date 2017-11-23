package com.flowy.fomoapi.services

import com.softwaremill.bootzooka.test.{FlatSpecWithDb, TestHelpersWithDb}
import org.scalatest.Matchers


class UserKeyServiceSpec extends FlatSpecWithDb with Matchers with TestHelpersWithDb {

  val testKey = "test_key"
  val userEmail = "apikeyuser@test.com"

  override protected def beforeAll() = {
    super.beforeAll()
    userDao.add(newUser("UserKeyServiceSpec", "Test", userEmail, "pass", "salt")).futureValue
  }

  "UserKeyService" should "add a new user's key" in {
    val user = userDao.findByEmail(userEmail).futureValue.get
    val future = userKeyService.addUserKey(user.id, "UserKeyServiceSpecEx", testKey, "secret", "this is a test key").futureValue
    future.isRight should be (true)
    future should matchPattern { case Right(_) => }
  }

  it should "not add a duplicate user key" in {
    val user = userDao.findByEmail(userEmail).futureValue.get
    val response = userKeyService.addUserKey(user.id, "UserKeyServiceSpecExDup", testKey, "secret", "this is a test key").futureValue
    response should matchPattern { case Left("user key already exists") => }
  }

  it should "retrieve a user key by user id" in {
    val user = userDao.findByEmail(userEmail).futureValue.get
    val ukey = userKeyService.getUserKey(user.id, testKey).futureValue.get

    ukey.userId should be(user.id)
    ukey.exchange should be("UserKeyServiceSpecEx")
    ukey.key should be(testKey)
  }

  it should "update a user key" in {
    val newKey = "new_key"
    val newSecret = "new_secret"
    val newDesc = "Update a user key test"

    val user = userDao.findByEmail(userEmail).futureValue
    val ukey = userKeyService.getUserKey(user.get.id, testKey).futureValue.get
    val status = userKeyService.update(ukey.copy(
      key = newKey,
      secret = newSecret,
      description = newDesc)).futureValue

    status should be(true)

    // expecting this update to succeeded so getting the new key via '.get' on a Option should succeed
    val updatedKey = userKeyService.getUserKey(user.get.id, newKey).futureValue.get

    // you can't update the id or userId
    updatedKey.id should be(ukey.id)
    updatedKey.userId should be(ukey.userId)
    updatedKey.exchange should be(ukey.exchange)

    // these should be updated
    updatedKey.key should be(newKey)
    updatedKey.secret should be(newSecret)
    updatedKey.description should be(newDesc)
  }
}
