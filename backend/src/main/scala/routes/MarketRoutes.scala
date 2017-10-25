package routes

import java.util.UUID
import javax.ws.rs.{GET, POST, Path}

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.flow.marketmaker.models.{SimpleConditionalFactory, TradeOrder, TradeType}
import com.flow.marketmaker.services.MarketService.PlaceOrder
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
          entity(as[BuyOrder]) { in =>
            val marketName = in.marketName
            val conditions = in.buyConditions.map{ c => SimpleConditionalFactory.makeCondition(c.operator, c.value) }

            val order = new TradeOrder(userId = user.id,
              exchangeName = in.exchangeName,
              marketName = in.marketName,
              currencyName = in.marketName.split("-")(1),
              side = TradeType.Buy,
              quantity = in.quantity,
              orConditions = conditions
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
  }

  def setSell = {
    path("setsell") {
      post {
        completeOk
      }
    }
  }
}

case class BuyCondition(conditionType: String, indicator: String, operator: String, value: Double)

case class BuyOrder(exchangeName: String,
                    marketName: String,
                    quantity: Double,
                    buyConditions: List[BuyCondition])
