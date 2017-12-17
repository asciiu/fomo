package com.flowy.fomoapi.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import com.flowy.fomoapi.services.UserKeyService
import com.flowy.fomoapi.services.UserService
import com.flowy.common.api.BittrexClient
import com.flowy.common.models._
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.user.api._
import com.typesafe.scalalogging.StrictLogging
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import java.time.{Instant, ZoneOffset}
import java.util.UUID

import com.flowy.common.services.BalanceService


case class ApiKey(exchange: String, key: String, secret: String, description: String)

trait ApiKeyRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  def userService: UserService
  def userKeyService: UserKeyService
  def userBalanceService: BalanceService
  def bittrexClient: BittrexClient

  def convertToUkey(userId: UUID, keyId: UUID, request: ApiKey): UserKey =
    UserKey(keyId,
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
      getKey ~
      deleteKey ~
      updateKey ~
      listKeys ~
      postKey
    }
  }

  def getKey =
    path(JavaUUID) { keyId =>
      get {
        userFromSession { user =>
          onSuccess(userKeyService.getUserKey(user.id, keyId)) {
            case Some(key) =>
              val lekey = UserKeyNoSecret.fromUserKey(key)

              complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", Map[JsonKey, UserKeyNoSecret](JsonKey("key") -> lekey).asJson))
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
  def listKeys =
      get {
        userFromSession { user =>
          onSuccess(userKeyService.getAllKeys(user.id)){ keys =>

            case class UserKeySimple(id: UUID, exchange: Exchange.Value, key: String)
            val simpleKeys = keys.map { k => UserKeySimple(k.id, k.exchange, k.key)}

            complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", Map[JsonKey, Seq[UserKeySimple]](JsonKey("keys") -> simpleKeys).asJson))
          }
        }
    }

  def postKey =
    post {
      userFromSession{ user =>
        entity(as[ApiKey]) { key =>
          onSuccess(userKeyService.addUserKey(user.id, Exchange.withName(key.exchange.toLowerCase()), key.key, key.secret, key.description)) {
            case Left(msg) =>
              complete(StatusCodes.Conflict, JSendResponse(JsonStatus.Fail, msg, Json.Null))
            case Right(key)  =>
              val lekey = UserKeyNoSecret.fromUserKey(key)

              complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", Map[JsonKey, UserKeyNoSecret](JsonKey("key") -> lekey).asJson))
          }
        }
      }
    }

  def deleteKey = {
    path(JavaUUID) { keyId =>
      delete {
        userFromSession { user =>
          onSuccess(userKeyService.remove(user.id, keyId)) {
            case true =>
              complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", Json.Null))
            case false =>
              complete(StatusCodes.NotFound, JSendResponse(JsonStatus.Fail, "user key not found", Json.Null))
          }
        }
      }
    }
  }

  def updateKey = {
    path(JavaUUID) { keyId =>
      put {
        userFromSession { user =>
          entity(as[ApiKey]) { ukey =>
            onSuccess(userKeyService.update(convertToUkey(user.id, keyId, ukey))) {
              case Some(key) =>
                val lekey = UserKeyNoSecret.fromUserKey(key)

                complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", Map[JsonKey, UserKeyNoSecret](JsonKey("key") -> lekey).asJson))
              case _ =>
                complete(StatusCodes.NotFound, JSendResponse(JsonStatus.Fail, s"invalid key", Json.Null))
            }
          }
        }
      }
    }
  }
}
