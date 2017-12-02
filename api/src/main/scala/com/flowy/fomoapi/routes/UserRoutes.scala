package com.flowy.fomoapi.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.Directives._
import com.flowy.fomoapi.services.UserKeyService
import com.flowy.fomoapi.services.{UserRegisterResult, UserService}
import com.flowy.common.api.Bittrex.BalancesResponse
import com.flowy.common.api.{Auth, BittrexClient}
import com.flowy.common.utils.Utils
import com.flowy.common.models.{ApiKeyStatus, BasicUserData, Exchange, UserKey}
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.user.api._
import com.softwaremill.bootzooka.user.application.Session
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.typesafe.scalalogging.StrictLogging
import java.time.{Instant, ZoneOffset}
import java.util.UUID
import javax.ws.rs.{GET, POST, Path}
import io.circe.{Encoder, Json}
import io.circe.generic.auto._
import io.circe.syntax._
import io.swagger.annotations._
import scala.concurrent.Future


@Api(value = "/user", produces = "application/json")
@Path("/api/user")
trait UsersRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  def userService: UserService
  def userKeyService: UserKeyService
  def bittrexClient: BittrexClient

  implicit val basicUserDataCbs = CanBeSerialized[BasicUserData]

  def convertToUkey(userId: UUID, request: UpdateApiKeyRequest): UserKey =
    UserKey(request.id,
      userId,
      Exchange.withName(request.exchange),
      request.key,
      request.secret,
      request.description,
      ApiKeyStatus.Added,
      Instant.now().atOffset(ZoneOffset.UTC),
      Instant.now().atOffset(ZoneOffset.UTC)
    )

  val usersRoutes = logRequestResult("UserRoutes") {
    pathPrefix("user") {
      addApiKey ~
      balances ~
      changePassword ~
      changeUserEmail ~
      getApiKey ~
      loginUser ~
      logoutUser ~
      registerUser ~
      removeApiKey ~
      session ~
      updateApiKey
    }
  }

  implicit val encodeStatus: Encoder[ApiKeyStatus.Value] = new Encoder[ApiKeyStatus.Value] {
    final def apply(a: ApiKeyStatus.Value): Json = Json.obj(
      ("status", Json.fromString(a.toString))
    )
  }
  implicit val encodeExchange: Encoder[Exchange.Value] = new Encoder[Exchange.Value] {
    final def apply(a: Exchange.Value): Json = Json.obj(
      ("status", Json.fromString(a.toString))
    )
  }

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

  def addApiKey =
    path("apikey"){
      post {
        userFromSession{ user =>
          entity(as[ApiKey]) { key =>
            onSuccess(userKeyService.addUserKey(user.id, Exchange.withName(key.exchange), key.key, key.secret, key.description)) {
              case Left(msg) =>
                complete(StatusCodes.Conflict, JSendResponse(JsonStatus.Fail, msg, Json.Null))
              case Right(_)  =>

                completeOk
            }
          }
        }
      }
    }

  def getApiKey =
    path("apikey") {
      get {
        parameters('exchange) { exchangeName =>
          userFromSession { user =>
            onSuccess(userKeyService.getUserKey(user.id, Exchange.withName(exchangeName))) {
              case Some(key) =>
                complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", key.asJson))
              case _ =>
                complete(StatusCodes.NotFound, JSendResponse(JsonStatus.Fail, "Exchange key not found. Try adding it.", Json.Null))
            }
          }
        }
      }
    }

  def removeApiKey = {
    path("apikey") {
      delete {
        userFromSession { user =>
          entity(as[RemoveApiKeyRequest]) { request =>
            onSuccess(userKeyService.remove(user.id, Exchange.withName(request.exchange))) {
              case true =>
                complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", Json.Null))
              case false =>
                complete(StatusCodes.NotFound, JSendResponse(JsonStatus.Fail, "user key not found", Json.Null))
            }
          }
        }
      }
    }
  }

  def updateApiKey = {
    path("apikey") {
      put {
        userFromSession { user =>
          entity(as[UpdateApiKeyRequest]) { ukey =>
            onSuccess(userKeyService.update(convertToUkey(user.id, ukey))) {
              case true =>
                complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", Json.Null))
              case false =>
                complete(StatusCodes.NotFound, JSendResponse(JsonStatus.Fail, "invalid key entered", Json.Null))
            }
          }
        }
      }
    }
  }

  def balances =
    path("balances") {
      get {
        parameters('exchange) { exchangeName =>
          userFromSession { user =>
            onSuccess(userKeyService.getUserKey(user.id, Exchange.withName(exchangeName))) {
              case Some(ukey) =>
                val auth = Auth(ukey.key, ukey.secret)
                onSuccess(bittrexClient.accountGetBalances(auth)){
                  case balResponse: BalancesResponse =>
                    complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", balResponse.asJson))
                  case _ =>
                    complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", Json.Null))
                }
              case _ =>
                complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", Json.Null))
            }
          }
        }
      }
    }

  def changeUserEmail =
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

  def session =
    path("session") {
      get {
        userFromSession { user =>
          setSession(refreshable, usingHeaders, Session(user.id)) {
            complete(JSendResponse(JsonStatus.Success, "", Map[JsonKey, BasicUserData](JsonKey("user") -> user).asJson))
          }
        }
      }
    }
}


case class ApiKey(exchange: String, key: String, secret: String, description: String)
case class UpdateApiKeyRequest(id: UUID, exchange: String, key: String, secret: String, description: String)
case class RemoveApiKeyRequest(exchange: String)
case class RegistrationInput(first: String, last: String, email: String, password: String) {
  def firstEscaped = Utils.escapeHtml(first)
  def lastEscaped = Utils.escapeHtml(last)
}

case class ChangePasswordInput(currentPassword: String, newPassword: String)

case class LoginInput(email: String, password: String, rememberMe: Option[Boolean])

case class PatchUserInput(email: Option[String])
