package routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.test.{BaseRoutesSpec, TestHelpersWithDb}
import com.flowy.fomoapi.routes.UsersRoutes
import com.flowy.common.api.BittrexClient
import com.flowy.common.database.postgres.SqlTheEverythingBagelDao
import com.flowy.common.models.{ApiKeyStatus, Exchange, UserKey}
import com.flowy.fomoapi.services.UserKeyService

import scala.concurrent.Await
import scala.concurrent.duration._

class UsersRoutesSpec extends BaseRoutesSpec with TestHelpersWithDb with RoutesSupport { spec =>

  val routes = Route.seal(new UsersRoutes with TestRoutesSupport {
    override val userService = spec.userService
    override val bittrexClient = new BittrexClient()
    override val bagel = new SqlTheEverythingBagelDao(sqlDatabase)
    override val userKeyService = new UserKeyService(bagel)
    override val system = ActorSystem("test")
  }.usersRoutes)


  "POST /register" should "register new user" in {
    Post("/user/register", Map("first" -> "John", "last"-> "Doe", "email" -> "newUser@sml.com", "password" -> "secret")) ~> routes ~> check {
      userDao.findByEmail("newUser@sml.com").futureValue should be('defined)
      status should be(StatusCodes.OK)
    }
  }

  "POST /register with invalid data" should "result in an error" in {
    Post("/user/register") ~> routes ~> check {
      status should be(StatusCodes.BadRequest)
    }
  }

  "POST /user/whatever" should "not be bound to /user login - reject unmatchedPath request" in {
    Post("/user/whatever") ~> routes ~> check {
      status should be(StatusCodes.NotFound)
    }
  }

  "POST /register with an existing email" should "return 409 with an error message" in {
    userDao.add(newUser("user2", "2", "user2@sml.com", "pass", "salt")).futureValue
    Post("/user/register", Map("first" -> "Random", "last" -> "Person", "email" -> "user2@sml.com", "password" -> "secret")) ~> routes ~> check {
      status should be(StatusCodes.Conflict)
      val response = entityAs[JSendResponse]
      response.message should be("E-mail already in use!")
      response.status should be(JsonStatus.Fail)
    }
  }

  "POST /register" should "use escaped Strings" in {
    Post(
      "/user/register",
      Map("first" -> "<script>alert('haxor');</script>",
        "last" -> "<script>alert('haxor');</script>",
        "email" -> "boner@dot.com",
        "password" -> "secret")
    ) ~> routes ~> check {
      status should be(StatusCodes.OK)
      userDao.findByEmail("boner@dot.com").futureValue.map(_.firstName) should be(
        Some("&lt;script&gt;alert('haxor');&lt;/script&gt;")
      )
    }
  }

  def withLoggedInUser(email: String, password: String)(body: RequestTransformer => Unit) =
    Post("/user/login", Map("email" -> email, "password" -> password)) ~> routes ~> check {
      status should be(StatusCodes.OK)

      val Some(sessionHeader) = header("Set-Authorization")
      body(addHeader("Authorization", sessionHeader.value()))
    }

  "POST /user/login" should "log in given valid credentials" in {
    userDao.add(newUser("user3", "3", "user3@sml.com", "pass", "salt")).futureValue
    withLoggedInUser("user3@sml.com", "pass") { _ =>
      status should be(StatusCodes.OK)
    }
  }

  "POST /user" should "not log in given invalid credentials" in {
    userDao.add(newUser("user4", "4", "user4@sml.com", "pass", "salt")).futureValue
    Post("/user/login", Map("email" -> "user4@sml.com", "password" -> "hacker")) ~> routes ~> check {
      status should be(StatusCodes.Forbidden)
    }
  }

  "PATCH /user/changeemail" should "update email when email is given" in {
    userDao.add(newUser("user5", "5", "user5@sml.com", "pass", "salt")).futureValue
    val email = "coolmail@awesome.rox"

    withLoggedInUser("user5@sml.com", "pass") { transform =>
      Patch("/user/changeemail", Map("email" -> email)) ~> transform ~> routes ~> check {
        userDao.findByEmail(email).futureValue.map(_.email) should be(Some(email))
        status should be(StatusCodes.OK)
      }
    }
  }

  "PATCH /user" should "result in an error when user is not authenticated" in {
    Patch("/user/changeemail", Map("email" -> "?")) ~> routes ~> check {
      status should be(StatusCodes.Forbidden)
    }
  }

  "PATCH /user" should "result in an error in neither email nor login is given" in {
    userDao.add(newUser("user7", "7", "user7@sml.com", "pass", "salt")).futureValue
    withLoggedInUser("user7@sml.com", "pass") { transform =>
      Patch("/user/changeemail", Map.empty[String, String]) ~> transform ~> routes ~> check {
        status should be(StatusCodes.Conflict)
      }
    }
  }

  "POST /changepassword" should "update password if current is correct and new is present" in {
    userDao.add(newUser("user8", "8", "user8@sml.com", "pass", "salt")).futureValue
    withLoggedInUser("user8@sml.com", "pass") { transform =>
      Post("/user/changepassword", Map("currentPassword" -> "pass", "newPassword" -> "newPass")) ~> transform ~> routes ~> check {
        status should be(StatusCodes.OK)
      }
    }
  }

  "POST /changepassword" should "not update password if current is wrong" in {
    userDao.add(newUser("user9", "9", "user9@sml.com", "pass", "salt")).futureValue
    withLoggedInUser("user9@sml.com", "pass") { transform =>
      Post("/user/changepassword", Map("currentPassword" -> "hacker", "newPassword" -> "newPass")) ~> transform ~> routes ~> check {
        status should be(StatusCodes.Forbidden)
      }
    }
  }

  "POST /apikey" should "add a new user key" in {
    userDao.add(newUser("user10", "10", "user10@sml.com", "pass", "salt")).futureValue
    withLoggedInUser("user10@sml.com", "pass") { transform =>
      Post("/user/apikey", Map("key" -> "key", "exchange" -> "test", "secret" -> "sssh", "description" -> "testy key")) ~> transform ~> routes ~> check {
        status should be(StatusCodes.OK)
      }
    }
  }

  "POST /apikey" should "not add a dupe key" in {
    val key = "key"
    val secret = "secret_key"
    val email = "user11@sml.com"
    val user = newUser("user11", "11", email, "pass", "salt")
    val userKey = UserKey.withRandomUUID(user.id, Exchange.Bittrex, key, secret, "testy", ApiKeyStatus.Added)

    userDao.add(user)
    val future = userKeyDao.add(userKey)
    Await.ready(future, 5.second)

    withLoggedInUser(email, "pass") { transform =>
      Post("/user/apikey", Map("key" -> key, "exchange" -> "test", "secret" -> secret, "description" -> "testy key")) ~> transform ~> routes ~> check {
        status should be(StatusCodes.Conflict)
      }
    }
  }
}
