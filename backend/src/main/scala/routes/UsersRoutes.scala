package routes

import javax.ws.rs.Path

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.Directives._
import com.softwaremill.bootzooka.common.Utils
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.user.api._
import com.softwaremill.bootzooka.user.application.Session
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.typesafe.scalalogging.StrictLogging
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import io.swagger.annotations._
import models.BasicUserData
import services.{UserRegisterResult, UserService}

import scala.concurrent.Future


@Api(value = "/users", produces = "application/json")
@Path("/users")
trait UsersRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  def userService: UserService

  implicit val basicUserDataCbs = CanBeSerialized[BasicUserData]

  val usersRoutes = pathPrefix("users") {
    loginUser ~
    logoutUser ~
    registerUser ~
    changePassword ~
    changeuUserEmail ~
    basicUserInfo
  }

  @ApiOperation(value = "User Login", nickname = "login", httpMethod = "POST", response = classOf[JSendResponse])
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ok")
  ))
  def loginUser =
    path("login") {
      post {
        entity(as[LoginInput]) { in =>
          onSuccess(userService.authenticate(in.email, in.password)) {
            case None => reject(AuthorizationFailedRejection)
            case Some(user) =>
              val session = Session(user.id)
              (if (in.rememberMe.getOrElse(false)) {
                setSession(refreshable, usingHeaders, session)
              } else {
                setSession(oneOff, usingHeaders, session)
              }) {
                complete(JSendResponse(JsonStatus.Success, "", Map[JsonKey, BasicUserData](JsonKey("user") -> user).asJson))
              }
          }
        }
      }
    }

  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "Logs the user out")
  @ApiResponses(Array(
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def logoutUser =
    path("logout") {
      get {
        userIdFromSession { _ =>
          invalidateSession(refreshable, usingHeaders) {
            completeOk
          }
        }
      }
    }

  def registerUser =
    path("register") {
      post {
        entity(as[RegistrationInput]) { registration =>
          onSuccess(userService.registerNewUser(registration.firstEscaped, registration.lastEscaped, registration.email, registration.password)) {
            case UserRegisterResult.InvalidData(msg) =>
              complete(StatusCodes.BadRequest, JSendResponse(JsonStatus.Fail, msg, Json.Null))
            case UserRegisterResult.UserExists(msg)  =>
              complete(StatusCodes.Conflict, JSendResponse(JsonStatus.Fail, msg, Json.Null))
            case UserRegisterResult.Success          =>
              complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", Json.Null))
          }
        }
      }
    }

  def changePassword =
    path("changepassword") {
      post {
        userFromSession { user =>
          entity(as[ChangePasswordInput]) { in =>
            onSuccess(userService.changePassword(user.id, in.currentPassword, in.newPassword)) {
              case Left(msg) =>
                complete(StatusCodes.Forbidden, JSendResponse(JsonStatus.Fail, msg, Json.Null))
              case Right(_)  =>
                completeOk
            }
          }
        }
      }
    }

  def changeuUserEmail =
    path("changeemail") {
      patch {
        userIdFromSession { userId =>
          entity(as[PatchUserInput]) { in =>
            val updateAction = in.email match {
              case Some(email) => userService.changeEmail(userId, email)
              case _ => Future.successful(Left("You have to provide new login or email"))
            }

            onSuccess(updateAction) {
              case Left(msg) =>
                complete(StatusCodes.Conflict, JSendResponse(JsonStatus.Fail, msg, Json.Null))
              case Right(_) =>
                completeOk
            }
          }
        }
      }
    }

  def basicUserInfo =
    path("/") {
      get {
        userFromSession { user =>
          complete(JSendResponse(JsonStatus.Success, "", Map[JsonKey, BasicUserData](JsonKey("user") -> user).asJson))
        }
      }
    }
}

case class RegistrationInput(first: String, last: String, email: String, password: String) {
  def firstEscaped = Utils.escapeHtml(first)
  def lastEscaped = Utils.escapeHtml(last)
}

case class ChangePasswordInput(currentPassword: String, newPassword: String)

case class LoginInput(email: String, password: String, rememberMe: Option[Boolean])

case class PatchUserInput(email: Option[String])
