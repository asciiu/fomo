package com.flowy.fomoapi.services

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.flowy.common.database.postgres.SqlTheEverythingBagelDao
import com.flowy.common.models.Exchange
import com.flowy.common.utils.sql.SqlDatabase
import com.softwaremill.bootzooka.test.{FlatSpecWithDb, TestHelpersWithDb}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext


class UserKeyServiceSpec extends FlatSpecWithDb with Matchers with TestHelpersWithDb {

  val testKey = "test_key"
  val userEmail = "apikeyuser@test.com"
  val bittrex = Exchange.Bittrex

  implicit lazy val system = ActorSystem("test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  lazy val bagel                 = new SqlTheEverythingBagelDao(sqlDatabase)
  lazy val userKeyService        = new UserKeyService(bagel)

  override protected def beforeAll() = {
    super.beforeAll()
    userDao.add(newUser("UserKeyServiceSpec", "Test", userEmail, "pass", "salt")).futureValue
  }

  "UserKeyService" should "add a new user's key" in {
    val user = userDao.findByEmail(userEmail).futureValue.get
    val future = userKeyService.addUserKey(user.id, bittrex, testKey, "secret", "this is a test key").futureValue
    future.isRight should be (true)
    future should matchPattern { case Right(_) => }
  }

  it should "not add a duplicate user key" in {
    val user = userDao.findByEmail(userEmail).futureValue.get
    val response = userKeyService.addUserKey(user.id, bittrex, testKey, "secret", "this is a test key").futureValue
    response should matchPattern { case Left("user key already exists") => }
  }

  it should "retrieve all user keys" in {
    val user = userDao.findByEmail(userEmail).futureValue.get
    val keys = userKeyService.getAllKeys(user.id).futureValue

    keys.length should be(1)
    val ukey = keys(0)

    ukey.exchange should be(bittrex)
    ukey.key should be(testKey)
  }

  it should "update a user key" in {
    val newKey = "new_key"
    val newSecret = "new_secret"
    val newDesc = "Update a user key test"

    val user = userDao.findByEmail(userEmail).futureValue.get
    val keys = userKeyService.getAllKeys(user.id).futureValue
    val ukey = keys(0)

    val status = userKeyService.update(ukey.copy(
      key = newKey,
      secret = newSecret,
      description = newDesc)).futureValue

    // expecting this update to succeeded so getting the new key via '.get' on a Option should succeed
    val updatedKey = userKeyService.getUserKey(user.id, ukey.id).futureValue.get

    // you can't update the id or userId
    updatedKey.id should be(ukey.id)
    updatedKey.userId should be(ukey.userId)
    updatedKey.exchange should be(ukey.exchange)

    // these should be updated
    updatedKey.key should be(newKey)
    updatedKey.secret should be(newSecret)
    updatedKey.description should be(newDesc)
  }

  it should "not update a key that does not match the id, userId, and exchange name" in {
    val newKey = "new_key"
    val newSecret = "new_secret"
    val newDesc = "Update a user key test"

    val user = userDao.findByEmail(userEmail).futureValue.get
    val keys = userKeyService.getAllKeys(user.id).futureValue
    val ukey = keys(0)

    // if the id, userID, and exhange name are not found from a previous key pair
    // we cannot update a key
    val status = userKeyService.update(ukey.copy(
      userId = UUID.randomUUID(),
      exchange = Exchange.Poloniex,
      key = newKey,
      secret = newSecret,
      description = newDesc)).futureValue

    status should be(None)

  }
}
