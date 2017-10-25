package routes

import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Directives._
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.user.api.SessionSupport
import com.typesafe.scalalogging.StrictLogging

trait MarketRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  val marketRoutes = pathPrefix("market") {
    setBuy ~
    setSell
  }

  def setBuy = {
    path("setbuy") {
      post {
        completeOk
      }
    }
  }

  def setSell = {
    path("setbuy") {
      post {
        completeOk
      }
    }
  }
}
