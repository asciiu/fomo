package routes


import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.flow.bittrex.api.Bittrex.MarketResult
import com.flow.marketmaker.database.TheEverythingBagelDao
import com.flow.marketmaker.models.{Trade, TradeRequest}
import com.flow.marketmaker.services.MarketService.PostTrade
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.user.api.SessionSupport
import com.typesafe.scalalogging.StrictLogging
import io.circe.{Encoder, Json}
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.duration._

trait MarketRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  def bittrexService: ActorRef
  def bagel: TheEverythingBagelDao

  import com.flow.bittrex.BittrexService._
  import Trade._

  // TODO
  // when a trade does not execute successfully you need an error log to tell you why
  val marketRoutes = logRequestResult("MarketRoutes") {
    pathPrefix("market") {
      directory ~
      getTrade ~
      listTrades ~
      postTrade ~
      updateTrade
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


  def listTrades = {
    path("trades") {
      get {
        parameters('marketName.?, 'exchangeName.?, 'status.*) { (marketName, exchangeName, statusIter) =>
          userFromSession { user =>
            onSuccess(bagel.findTradesByUserId(user.id, marketName, exchangeName, statusIter.toList).mapTo[Seq[Trade]]) {
              case trades: Seq[Trade] =>
                complete(JSendResponse(JsonStatus.Success, "", trades.asJson))
              case _ =>
                complete(StatusCodes.NotFound, JSendResponse(JsonStatus.Fail, "user trades not found", Json.Null))
            }
          }
        }
      }
    }
  }

  def getTrade = {
    path("trade" / JavaUUID) { tradeId =>
      get {
        userFromSession { user =>
          onSuccess( bagel.findTradeById(tradeId).mapTo[Option[Trade]] ) {
            case Some(trade) =>
              complete(JSendResponse(JsonStatus.Success, "", trade.asJson))
            case _ =>
              complete(StatusCodes.NotFound, JSendResponse(JsonStatus.Fail, "trade not found", Json.Null))
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
            implicit val timeout = Timeout(1.second)

            onSuccess( (bittrexService ? PostTrade(user, tradeRequest)).mapTo[Boolean] ) {
              case true =>
                completeOk
              case _ =>
                complete(StatusCodes.Conflict, JSendResponse(JsonStatus.Fail, "trade not posted", Json.Null))
            }
          }
        }
      }
    }
  }

  def updateTrade = {
    path("trade") {
      put {
        userFromSession { user =>
          entity(as[Trade]) { trade =>
            implicit val timeout = Timeout(1.second)

            onSuccess( bagel.updateTrade(trade).mapTo[Boolean] ) {
              case true =>
                completeOk
              case _ =>
                complete(StatusCodes.Conflict, JSendResponse(JsonStatus.Fail, "trade not posted", Json.Null))
            }
          }
        }
      }
    }

  }
}
