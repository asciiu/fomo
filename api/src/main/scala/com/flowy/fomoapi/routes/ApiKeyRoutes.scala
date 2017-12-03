package com.flowy.fomoapi.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.Directives._
import com.flowy.fomoapi.services.UserKeyService
import com.flowy.fomoapi.services.{UserRegisterResult, UserService}
import com.flowy.common.api.Bittrex.{BalanceResponse, BalancesResponse}
import com.flowy.common.api.{Auth, BittrexClient}
import com.flowy.common.utils.Utils
import com.flowy.common.models._
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

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future


trait ApiKeyRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  def userService: UserService
  def userKeyService: UserKeyService
  def bittrexClient: BittrexClient

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

  val apiKeyRoutes = logRequestResult("ApiKeyRoutes") {
    pathPrefix("keys") {
      addApiKey ~
      getApiKey ~
      removeApiKey ~
      updateApiKey ~
      listApiKeys
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
    path(JavaUUID) { keyId =>
      get {
        userFromSession { user =>
          onSuccess(userKeyService.getUserKey(user.id, keyId)) {
            case Some(key) =>
              complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", key.asJson))
            case _ =>
              complete(StatusCodes.NotFound, JSendResponse(JsonStatus.Fail, s"key $keyId not found.", Json.Null))
          }
        }
      }
    }

  /**
    * This route must come last in the list of apiKeyRoutes up top
    * @return
    */
  def listApiKeys =
      get {
        userFromSession { user =>
          onSuccess(userKeyService.getAllKeys(user.id)){ keys =>

            case class UserKeySimple(id: UUID, exchange: Exchange.Value, key: String)
            val simpleKeys = keys.map { k => UserKeySimple(k.id, k.exchange, k.key)}

            complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", Map[JsonKey, Seq[UserKeySimple]](JsonKey("keys") -> simpleKeys).asJson))
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
}


case class ApiKey(exchange: String, key: String, secret: String, description: String)
case class UpdateApiKeyRequest(id: UUID, exchange: String, key: String, secret: String, description: String)
case class RemoveApiKeyRequest(exchange: String)
