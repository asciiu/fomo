package com.flowy.fomoapi.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.Directives._
import com.flowy.fomoapi.services.UserKeyService
import com.flowy.fomoapi.services.{UserRegisterResult, UserService}
import com.flowy.common.api.Bittrex.BalancesResponse
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

import akka.actor.ActorSystem
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import com.flowy.cache.CacheService.CacheBittrexBalances
import io.circe.{Encoder, Json}
import io.circe.generic.auto._
import io.circe.syntax._

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future


trait UsersRoutes extends RoutesSupport with StrictLogging with SessionSupport {


  def system: ActorSystem
  def userService: UserService
  def userKeyService: UserKeyService
  def bittrexClient: BittrexClient

  lazy val mediator = DistributedPubSub(system).mediator

  val usersRoutes = logRequestResult("UserRoutes") {
    pathPrefix("user") {
      changePassword ~
      changeUserEmail ~
      loginUser ~
      logoutUser ~
      registerUser ~
      session
    }
  }

  def loginUser =
    path("login") {
      post {
        entity(as[LoginInput]) { in =>
          onSuccess(userService.authenticate(in.email, in.password)) {
            case None => reject(AuthorizationFailedRejection)
            case Some(user) =>
              onSuccess(checkBalances(user.id)) { exchanges =>
                val session = Session(user.id)
                (if (in.rememberMe.getOrElse(false)) {
                  setSession(refreshable, usingHeaders, session)
                } else {
                  setSession(oneOff, usingHeaders, session)
                }) {
                  complete(JSendResponse(JsonStatus.Success, "", Map[JsonKey, UserData](JsonKey("user") -> user.copy(exchanges = exchanges)).asJson))
                }
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

  private def checkBalances(userId: UUID): Future[List[ExchangeData]] = {
    userKeyService.getAllKeys(userId).flatMap { keys =>
      val futures = new ListBuffer[Future[BalancesResponse]]()

      keys.foreach { key =>
        if (key.exchange == Exchange.Bittrex) {
          futures.append(bittrexClient.accountGetBalances(Auth(key.key, key.secret)))
        }
      }

      val exchanges = Future.sequence(futures.toList).map { listResponses =>
        listResponses.map { std =>
          std.result match {
            case Some(balances) =>
              mediator ! Publish("CacheBittrexBalances", CacheBittrexBalances(userId, balances))
              ExchangeData(Exchange.Bittrex, balances)
            case None =>
              ExchangeData(Exchange.Bittrex, Seq.empty[Balance])
          }
        }
      }
      exchanges
    }
  }

  def session =
    path("session") {
      get {
        userFromSession { user =>
          onSuccess(checkBalances(user.id)) { exchanges =>
            setSession(refreshable, usingHeaders, Session(user.id)) {
              complete(JSendResponse(JsonStatus.Success, "", Map[JsonKey, UserData](JsonKey("user") -> user.copy(exchanges = exchanges)).asJson))
            }
          }
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
