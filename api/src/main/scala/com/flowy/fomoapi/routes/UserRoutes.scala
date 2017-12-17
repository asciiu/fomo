package com.flowy.fomoapi.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.Directives._
import com.flowy.fomoapi.services.UserKeyService
import com.flowy.fomoapi.services.{UserRegisterResult, UserService}
import com.flowy.common.api.Bittrex.{BalancesAuthorization, BalancesResponse, ExchangeBalance}
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
import com.flowy.common.database.TheEverythingBagelDao
import io.circe.{Encoder, Json}
import io.circe.generic.auto._
import io.circe.syntax._

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future, Promise}


trait UsersRoutes extends RoutesSupport with StrictLogging with SessionSupport {


  def system: ActorSystem
  def userService: UserService
  def userKeyService: UserKeyService
  def bittrexClient: BittrexClient
  def bagel: TheEverythingBagelDao

  lazy val mediator = DistributedPubSub(system).mediator

  implicit def exchangeBalConversion(exbs: List[ExchangeBalance]): List[Balance] = {
    exbs.map { exb =>
      val exchangeAvailable = exb.exchangeAvailableBalance

      Balance(currency = exb.currency,
        availableBalance = exchangeAvailable,
        exchangeTotalBalance = exb.exchangeTotalBalance,
        exchangeAvailableBalance = exchangeAvailable,
        pending = exb.pending,
        cryptoAddress = exb.cryptoAddress)
    }
  }


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
              val ex = checkBalances(user.id)
              val dv = bagel.findUserDevices(user.id)

              val future = for {
                exs <- ex
                dvs <- dv
              } yield { user.copy(devices = dvs, exchanges = exs) }


              onSuccess(future) { userData =>
                val session = Session(userData.id)
                (if (in.rememberMe.getOrElse(false)) {
                  setSession(refreshable, usingHeaders, session)
                } else {
                  setSession(oneOff, usingHeaders, session)
                }) {
                  complete(JSendResponse(JsonStatus.Success, "", Map[JsonKey, UserData](JsonKey("user") -> userData).asJson))
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
    // TODO move this to utils
    def singleFuture[A](futures: List[Future[A]]): Future[List[A]] = {

      val p = Promise[List[A]]()
      p.success(List.empty[A])

      val f = p.future // a future containing empty list.

      futures.foldRight(f) {
        (fut, accum) =>  // foldRight means accumulator is on right.

          for {
            list <- accum;  // take List[A] out of Future[List[A]]
            a    <- fut     // take A out of Future[A]
          }
            yield (a :: list)   // A :: List[A]
      }
    }

    userKeyService.getAllKeys(userId).flatMap { keys =>
      val futures = new ListBuffer[Future[BalancesAuthorization]]()

      keys.foreach { key =>
        if (key.exchange == Exchange.Bittrex) {
          val future = bittrexClient.accountGetBalances(Auth(key.id, key.key, key.secret))
          futures.append(future)
        }
      }

      val exchanges = singleFuture[BalancesAuthorization](futures.toList).map { balAuth =>

        balAuth.map { std =>
          std.response.result match {
            case Some(exBalances) =>
              mediator ! Publish("CacheBittrexBalances", CacheBittrexBalances(userId, exBalances))

              ExchangeData(std.auth.apiKey, Exchange.Bittrex, exBalances)
            case None =>
              ExchangeData(std.auth.apiKey, Exchange.Bittrex, Seq.empty[Balance])
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
