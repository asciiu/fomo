package com.flowy.fomoapi.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import com.flowy.fomoapi.services.PasswordResetService
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.user.api.SessionSupport
import io.circe.Json
import io.circe.generic.auto._

trait PasswordResetRoutes extends RoutesSupport with SessionSupport {

  def passwordResetService: PasswordResetService

  val passwordResetRoutes = pathPrefix("passwordreset") {
    forgotPassword ~
    resetPassword
  }

  def resetPassword =
    post {
      path(Segment) { code =>
        entity(as[PasswordResetInput]) { in =>
          onSuccess(passwordResetService.performPasswordReset(code, in.password)) {
            case Left(e) =>
              complete(StatusCodes.Forbidden, JSendResponse(JsonStatus.Fail, e, Json.Null))
            case _ =>
              completeOk
          }
        }
      }
    }

  def forgotPassword =
    post {
      entity(as[ForgotPasswordInput]) { in =>
        onSuccess(passwordResetService.sendResetCodeToUser(in.email)) {
          complete(StatusCodes.Accepted, JSendResponse(JsonStatus.Success, "", Json.Null))
        }
      }
    }
}

case class PasswordResetInput(password: String)

case class ForgotPasswordInput(email: String)
