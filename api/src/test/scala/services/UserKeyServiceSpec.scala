package services

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
    val future1 = userKeyService.addUserKey(user.get.id, "test", "key", "secret", "this is a test key").futureValue
    val future2 = userKeyService.addUserKey(user.get.id, "test", "key", "secret", "this is a test key").futureValue

    future2 should matchPattern { case Left("user key already exists") => }
  }

  it should "retrieve a user key by user id" in {
    val key = "test_key"
    val user = userDao.findByEmail("apikeyuser2@test.com").futureValue
    val future = userKeyService.addUserKey(user.get.id, "test", key, "secret", "this is a test key")
    Await.ready(future, 5.second)

    val future2 = userKeyService.getUserKey(user.get.id, key).futureValue

    future2.isDefined should be (true)
    future2.get.key should be (key)
  }
}
