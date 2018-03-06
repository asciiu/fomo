package routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.flowy.common.api.BittrexClient
import com.flowy.common.database.postgres.SqlTheEverythingBagelDao
import com.flowy.common.models.{ApiKeyStatus, Exchange, UserKey}
import com.flowy.common.services.BalanceService
import com.flowy.fomoapi.routes.{ApiKeyRoutes, UsersRoutes}
import com.flowy.fomoapi.services.UserKeyService
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.test.{BaseRoutesSpec, TestHelpersWithDb}

import akka.http.scaladsl.server.Directives._
import scala.concurrent.Await
import scala.concurrent.duration._

class ApiKeyRoutesSpec extends BaseRoutesSpec with TestHelpersWithDb with RoutesSupport { spec =>

  val router = new ApiKeyRoutes with UsersRoutes with TestRoutesSupport {
    override val userService = spec.userService
    override val bittrexClient = new BittrexClient()

   // lazy val config = new DatabaseConfig {
   //   override def rootConfig = ConfigFactory.load()
   // }
   // val sqlDatabase = SqlDatabase.create(config)

    val bagel = new SqlTheEverythingBagelDao(sqlDatabase)
    override val userKeyService = new UserKeyService(bagel)
    override val userBalanceService = new BalanceService(bagel)
    val system = ActorSystem("test")
    lazy val routes =
      apiKeyRoutes ~ usersRoutes
  }

  val routes = Route.seal(router.routes)

  def withLoggedInUser(email: String, password: String)(body: RequestTransformer => Unit) =
    Post("/user/login", Map("email" -> email, "password" -> password)) ~> routes ~> check {

      status should be(StatusCodes.OK)
      val Some(sessionHeader) = header("Set-Authorization")
      body(addHeader("Authorization", sessionHeader.value()))
    }

  "POST /keys" should "add a new user key" in {
    userDao.add(newUser("user10", "10", "user10@sml.com", "pass", "salt")).futureValue
    withLoggedInUser("user10@sml.com", "pass") { transform =>
      Post("/keys", Map("key" -> "key", "exchange" -> "test", "secret" -> "sssh", "description" -> "testy key")) ~> transform ~> routes ~> check {
        status should be(StatusCodes.OK)
      }
    }
  }

  "POST /keys" should "not add a dupe key" in {
    val key = "key"
    val secret = "secret_key"
    val email = "user11@sml.com"
    val user = newUser("user11", "11", email, "pass", "salt")
    val userKey = UserKey.withRandomUUID(user.id, Exchange.Bittrex, key, secret, "testy", ApiKeyStatus.Added)

    userDao.add(user)
    val future = userKeyDao.add(userKey)
    Await.ready(future, 5.second)

    withLoggedInUser(email, "pass") { transform =>
      Post("/keys", Map("key" -> key, "exchange" -> "test", "secret" -> secret, "description" -> "testy key")) ~> transform ~> routes ~> check {
        status should be(StatusCodes.Conflict)
      }
    }
  }
}
