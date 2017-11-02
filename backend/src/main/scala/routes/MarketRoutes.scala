package routes

import java.time.OffsetDateTime
import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.flow.bittrex.BittrexService.GetMarkets
import com.flow.bittrex.api.Bittrex.MarketResult
import com.flow.marketmaker.models.{BuyOrder, TradeRequest}
import com.flow.marketmaker.services.MarketService.CreateOrder
import com.flow.marketmaker.services.services.actors.MarketSupervisor.GetMarketActorRef
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.user.api.SessionSupport
import com.typesafe.scalalogging.StrictLogging
import io.circe.Json
import io.circe.generic.auto._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.duration._

trait MarketRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  def bittrexService: ActorRef

  val marketRoutes = logRequestResult("MarketRoutes") {
    pathPrefix("market") {
      directory ~
      postTrade ~
      setBuy ~
      setSell
    }
  }

  case class MarketBasicInfo(marketName: String,
                             currency: String,
                             currencyLong: String,
                             baseCurrency: String,
                             baseCurrencyLong: String,
                             exchangeName: String)

  def directory = {
    path("directory") {
      get {
        parameters('name.?) { name =>
          userFromSession { user =>
            implicit val timeout = Timeout(1.second)
            val marketResults = (bittrexService ? GetMarkets(name)).mapTo[List[MarketResult]]
            onSuccess(marketResults) {
              case list: List[MarketResult] =>
                val info = list.map(x =>
                  MarketBasicInfo(x.MarketName,
                    x.MarketCurrency,
                    x.MarketCurrencyLong,
                    x.BaseCurrency,
                    x.BaseCurrencyLong,
                    "Bittrex"))

                complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", info.asJson))
              case _ =>
                complete(StatusCodes.NotFound, JSendResponse(JsonStatus.Fail, "resource not available", Json.Null))
            }
          }
        }
      }
    }
  }

  def postTrade = {
    path("trade") {
      post {
        userFromSession { user =>
          entity(as[TradeRequest]) { tradeRequest =>
            println(tradeRequest)
            completeOk
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
