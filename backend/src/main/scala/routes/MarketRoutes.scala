package routes

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.flow.marketmaker.models.{LessThanEq, TradeOrder, TradeType}
import com.flow.marketmaker.services.MarketService.PlaceOrder
import com.flow.marketmaker.services.services.actors.MarketSupervisor.GetMarketActorRef
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.user.api.SessionSupport
import com.typesafe.scalalogging.StrictLogging
import io.circe.Json

import scala.concurrent.duration._

trait MarketRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  def bittrexMarketSuper: ActorRef

  val marketRoutes = pathPrefix("market") {
    setBuy ~
    setSell
  }

  def setBuy = {
    path("setbuy") {
      post {
        userFromSession { user =>
          println(user.id)
          val condition = LessThanEq(0.00532700)
          val marketName = "BTC-NEO"
          val order = new TradeOrder(userId = user.id,
            exchangeName = "bittrex",
            marketName = marketName,
            currencyName = "NEO",
            side = TradeType.Buy,
            quantity = 5,
            orConditions = List(condition)
          )

          implicit val timeout = Timeout(1.second)

          onSuccess((bittrexMarketSuper ? GetMarketActorRef(marketName)).mapTo[Option[ActorRef]]) { opt =>
            opt match {
              case Some(marketActor) =>
                marketActor ! PlaceOrder(order)
                completeOk
              case None =>
                val msg = s"attempted to place order in unknown market ${marketName}"
                logger.info(msg)
                complete(StatusCodes.Conflict, JSendResponse(JsonStatus.Fail, msg, Json.Null))
            }
          }
        }
      }
    }
  }

  def setSell = {
    path("setsell") {
      post {
        completeOk
      }
    }
  }
}
