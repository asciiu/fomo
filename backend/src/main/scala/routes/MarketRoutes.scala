package routes

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.flow.marketmaker.models.BuyOrder
import com.flow.marketmaker.services.MarketService.CreateOrder
import com.flow.marketmaker.services.services.actors.MarketSupervisor.GetMarketActorRef
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.user.api.SessionSupport
import com.typesafe.scalalogging.StrictLogging
import io.circe.Json
import io.circe.generic.auto._

import scala.concurrent.duration._

trait MarketRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  def bittrexMarketSuper: ActorRef

  val marketRoutes = pathPrefix("market") {
    setBuy ~
    setSell
  }

  /**
    * {
    "exchangeName": "Bittrex",
    "marketName": "BTC-BAT",
    "quantity": 1000,
    "buyConditions": [
    	{
    	"conditionType": "simpleConditional",
    	"indicator": "price",
    	"operator": "<=",
    	"value": 0.00002800

    	}
    ]
    }
    * @return
    */
  def setBuy = {
    path("setbuy") {
      post {
        userFromSession { user =>
          entity(as[BuyOrder]) { buyOrder =>
            implicit val timeout = Timeout(1.second)

            onSuccess((bittrexMarketSuper ? GetMarketActorRef(buyOrder.marketName)).mapTo[Option[ActorRef]]) { opt =>
              opt match {
                case Some(marketActor) =>
                  marketActor ! CreateOrder(user, buyOrder)
                  completeOk
                case None =>
                  val msg = s"market not found! ${buyOrder.marketName}"
                  logger.warn(msg)
                  complete(StatusCodes.NotFound, JSendResponse(JsonStatus.Fail, msg, Json.Null))
              }
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
