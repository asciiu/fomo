package routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.user.api.SessionSupport
import io.circe.generic.auto._
import services.PasswordResetService

trait PasswordResetRoutes extends RoutesSupport with SessionSupport {

  def passwordResetService: PasswordResetService

  val passwordResetRoutes = pathPrefix("passwordreset") {
    post {
      path(Segment) { code =>
        entity(as[PasswordResetInput]) { in =>
          onSuccess(passwordResetService.performPasswordReset(code, in.password)) {
            case Left(e) => complete(StatusCodes.Forbidden, e)
            case _       => completeOk
          }
        }
      } ~ entity(as[ForgotPasswordInput]) { in =>
        onSuccess(passwordResetService.sendResetCodeToUser(in.email)) {
          complete("success")
        }
      }
    }
  }
}

case class PasswordResetInput(password: String)

case class ForgotPasswordInput(email: String)
