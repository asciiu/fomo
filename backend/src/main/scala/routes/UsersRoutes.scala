package routes

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
import io.circe.generic.auto._
import models.BasicUserData
import services.{UserRegisterResult, UserService}

import scala.concurrent.Future

trait UsersRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  def userService: UserService

  implicit val basicUserDataCbs = CanBeSerialized[BasicUserData]

  val usersRoutes = pathPrefix("users") {
    path("logout") {
      get {
        userIdFromSession { _ =>
          invalidateSession(refreshable, usingHeaders) {
            completeOk
          }
        }
      }
    } ~
      path("register") {
        post {
          entity(as[RegistrationInput]) { registration =>
            onSuccess(userService.registerNewUser(registration.firstEscaped, registration.lastEscaped, registration.email, registration.password)) {
              case UserRegisterResult.InvalidData(msg) => complete(StatusCodes.BadRequest, msg)
              case UserRegisterResult.UserExists(msg)  => complete(StatusCodes.Conflict, msg)
              case UserRegisterResult.Success          => complete("success")
            }
          }
        }
      } ~
      path("changepassword") {
        post {
          userFromSession { user =>
            entity(as[ChangePasswordInput]) { in =>
              onSuccess(userService.changePassword(user.id, in.currentPassword, in.newPassword)) {
                case Left(msg) => complete(StatusCodes.Forbidden, msg)
                case Right(_)  => completeOk
              }
            }
          }
        }
      } ~
      pathEnd {
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
                  complete(user)
                }
            }
          }
        } ~
          get {
            userFromSession { user =>
              complete(user)
            }
          } ~
          patch {
            userIdFromSession { userId =>
              entity(as[PatchUserInput]) { in =>
                val updateAction = in.email match {
                  case Some(email) => userService.changeEmail(userId, email)
                  case _           => Future.successful(Left("You have to provide new login or email"))
                }

                onSuccess(updateAction) {
                  case Left(msg) => complete(StatusCodes.Conflict, msg)
                  case Right(_)  => completeOk
                }
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
