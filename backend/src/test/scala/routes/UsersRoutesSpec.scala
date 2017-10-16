package routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.test.{BaseRoutesSpec, TestHelpersWithDb}

class UsersRoutesSpec extends BaseRoutesSpec with TestHelpersWithDb with RoutesSupport { spec =>

  val routes = Route.seal(new UsersRoutes with TestRoutesSupport {
    override val userService = spec.userService
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

  "POST /user/whatever" should "not be bound to /users login - reject unmatchedPath request" in {
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
        "email" -> "newUser@sml.com",
        "password" -> "secret")
    ) ~> routes ~> check {
      status should be(StatusCodes.OK)
      userDao.findByEmail("newUser@sml.com").futureValue.map(_.firstName) should be(
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
      // ok
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
}
