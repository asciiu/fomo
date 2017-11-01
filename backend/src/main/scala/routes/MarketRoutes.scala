package routes

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.flow.bittrex.BittrexService.GetMarkets
import com.flow.bittrex.api.Bittrex.MarketResult
import com.flow.marketmaker.models.BuyOrder
import com.flow.marketmaker.services.MarketService.CreateOrder
import com.flow.marketmaker.services.services.actors.MarketSupervisor.GetMarketActorRef
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.user.api.SessionSupport
import com.typesafe.scalalogging.StrictLogging
import io.circe.Json
import io.circe.generic.auto._
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._


import scala.concurrent.duration._

trait MarketRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  def bittrexService: ActorRef

  val marketRoutes = logRequestResult("MarketRoutes") {
    pathPrefix("market") {
      directory ~
      setBuy ~
      setSell
    }
  }

  case class MarketBasicInfo(marketName: String, marketFullName: String, exchangeName: String)

  def directory = {
    path("directory") {
      get {
        parameters('name.?) { name =>
          userFromSession { user =>
            implicit val timeout = Timeout(1.second)
            val marketResults = (bittrexService ? GetMarkets(name)).mapTo[List[MarketResult]]
            onSuccess(marketResults) {
              case list: List[MarketResult] =>
                val info = list.map(x => MarketBasicInfo(x.MarketName, x.MarketCurrencyLong, "Bittrex"))

                complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", info.asJson))
              case _ =>
                complete(StatusCodes.NotFound, JSendResponse(JsonStatus.Fail, "resource not available", Json.Null))
            }
          }
        }
      }
    }
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
            bittrexService ! CreateOrder(user, buyOrder)
            completeOk
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
