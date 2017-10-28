package routes

import javax.ws.rs.{GET, POST, Path}

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
import services.{UserKeyService, UserRegisterResult, UserService}

import scala.concurrent.Future


@Api(value = "/user", produces = "application/json")
@Path("/api/user")
trait UsersRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  def userService: UserService
  def userKeyService: UserKeyService

  implicit val basicUserDataCbs = CanBeSerialized[BasicUserData]

  val usersRoutes = logRequestResult("UserRoutes") {
    pathPrefix("user") {
      addApiKey ~
        basicUserInfo ~
        changePassword ~
        changeuUserEmail ~
        loginUser ~
        logoutUser ~
        registerUser
    }
  }

  @POST
  @Path("/login")
  @ApiOperation(value = "Login for user",
    notes = "Returns set-authoriation and optional set-refresh-token headers. Subsequent requests will need these headers set for authentication.",
    response = classOf[BasicUserData])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "login credentials", required = true,
      dataTypeClass = classOf[LoginInput], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 403, message = "The supplied authentication is not authorized to access this resource")
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
                complete(JSendResponse(JsonStatus.Success, "", Map[JsonKey, BasicUserData](JsonKey("basicUserData") -> user).asJson))
              }
          }
        }
      }
    }

  @GET
  @Path("/logout")
  @ApiOperation(value = "User logout",
    response = classOf[JSendResponse])
  @ApiResponses(Array(
    new ApiResponse(code = 403, message = "The supplied authentication is not authorized to access this resource")
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

  @GET
  @Path("/register")
  @ApiOperation(value = "Registers a new user",
    response = classOf[JSendResponse])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "registration deets", required = true,
      dataTypeClass = classOf[RegistrationInput], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "The request contains bad syntax or cannot be fulfilled."),
    new ApiResponse(code = 409, message = "Email already taken")
  ))
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

  @GET
  @Path("/changepassword")
  @ApiOperation(value = "Changes a user's password",
    response = classOf[JSendResponse])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "new password", required = true,
      dataTypeClass = classOf[ChangePasswordInput], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 403, message = "The supplied authentication is not authorized to access this resource")
  ))
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

  def addApiKey =
    path("apikey"){
      post {
        userFromSession{ user =>
          entity(as[ApiKey]) { key =>
            onSuccess(userKeyService.addUserKey(user.id, key.key, key.secret, key.description)) {
              case Left(msg) =>
                complete(StatusCodes.Conflict, JSendResponse(JsonStatus.Fail, msg, Json.Null))
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

  @GET
  @Path("/info")
  @ApiOperation(value = "Returns basic user info",
    response = classOf[JSendResponse])
  @ApiResponses(Array(
    new ApiResponse(code = 403, message = "The supplied authentication is not authorized to access this resource")
  ))
  def basicUserInfo =
    path("info") {
      get {
        userFromSession { user =>
          complete(JSendResponse(JsonStatus.Success, "", Map[JsonKey, BasicUserData](JsonKey("user") -> user).asJson))
        }
      }
    }
}


case class ApiKey(key: String, secret: String, description: String)
case class RegistrationInput(first: String, last: String, email: String, password: String) {
  def firstEscaped = Utils.escapeHtml(first)
  def lastEscaped = Utils.escapeHtml(last)
}

case class ChangePasswordInput(currentPassword: String, newPassword: String)

case class LoginInput(email: String, password: String, rememberMe: Option[Boolean])

case class PatchUserInput(email: Option[String])
