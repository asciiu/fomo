package com.flowy.fomoapi.routes

import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.pattern.ask
import akka.util.Timeout
import com.flowy.bexchange.ExchangeService.GetMarkets
import com.flowy.bexchange.MarketTradeService.{DeleteTrade, PostTrade, UpdateTrade}
import com.flowy.common.api.Bittrex.MarketResult
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models._
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.user.api.SessionSupport
import com.typesafe.scalalogging.StrictLogging
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import redis.RedisClient

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait TradeRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  def bittrexService: ActorRef
  def bagel: TheEverythingBagelDao
  def redis: RedisClient

  import Trade._

  // TODO
  // when a trade does not execute successfully you need an error log to tell you why
  val tradeRoutes = logRequestResult("TradeRoutes") {
    pathPrefix("trades") {
      tradeHistory ~
      deleteTrade ~
      getTrade ~
      directory ~
      updateTrade ~
      postTrade ~
      listTrades
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
            implicit val timeout = Timeout(2.second)
            val marketResults = (bittrexService ? GetMarkets(name)).mapTo[Seq[Market]]
            onSuccess(marketResults) {
              case seq: Seq[Market] =>
                val info = seq.map(x =>
                  MarketBasicInfo(x.marketName,
                    x.currency,
                    x.currencyLong,
                    x.baseCurrency,
                    x.baseCurrencyLong,
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

  def deleteTrade = {
    path(JavaUUID) { tradeId =>
      delete {
        userFromSession { user =>
          onSuccess(bagel.findTradeById(tradeId)) {
            case Some(trade) if (trade.userId == user.id && trade.status != TradeStatus.Sold) =>

              onSuccess( (bittrexService ? DeleteTrade(trade))(2.second).mapTo[Option[Trade]] ) {
                case Some(trade) =>
                  complete(JSendResponse(JsonStatus.Success, "", trade.asJson))
                case _ =>
                  complete(StatusCodes.NotFound, JSendResponse(JsonStatus.Fail, "trade not found", Json.Null))
              }
            case Some(trade) if (trade.userId == user.id && trade.status == TradeStatus.Sold) =>
              complete(StatusCodes.Conflict, JSendResponse(JsonStatus.Fail, "sold trades cannot be cancelled", Json.Null))

            case _ =>
              complete(StatusCodes.NotFound, JSendResponse(JsonStatus.Fail, "trade not found", Json.Null))
          }
        }
      }
    }
  }

  def tradeHistory = {
    path("history") {
      get {
        parameters('marketName.?, 'exchangeName.?) { (marketNameOpt, exchangeNameOpt) =>
          userFromSession { user =>
            onSuccess(bagel.findTradeHistoryByUserId(user.id, exchangeNameOpt, marketNameOpt).mapTo[Seq[TradeHistory]]) {
              case history: Seq[TradeHistory] =>
                complete(JSendResponse(JsonStatus.Success, "", history.asJson))
              case _ =>
                complete(StatusCodes.NotFound, JSendResponse(JsonStatus.Fail, "user trade history not found", Json.Null))
            }
          }
        }
      }
    }
  }

  def listTrades = {
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

  def getTrade = {
    path(JavaUUID) { tradeId =>
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
    post {
      userFromSession { user =>
        entity(as[TradeRequest]) { tradeRequest =>
          implicit val timeout = Timeout(1.second)

          Try(UUID.fromString(tradeRequest.apiKeyId)) match {

            case Success(apiKeyId) =>
              val currencyName = tradeRequest.marketName.split("-")(0)
              // check base currency balance
              onSuccess(bagel.findBalance(user.id, apiKeyId, currencyName)) {
                case Some(balance) if balance.availableBalance > tradeRequest.baseQuantity =>

                  onSuccess((bittrexService ? PostTrade(user, tradeRequest)).mapTo[Option[Trade]]) {
                    case Some(trade) =>
                      complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", trade.asJson))
                    case None =>
                      complete(StatusCodes.Conflict, JSendResponse(JsonStatus.Fail, "trade not posted", Json.Null))
                  }
                case _ =>
                  complete(StatusCodes.UnprocessableEntity, JSendResponse(JsonStatus.Fail, s"user available balance is less than baseQuantity", Json.Null))
              }
            case Failure(_) =>
              complete(StatusCodes.UnprocessableEntity, JSendResponse(JsonStatus.Fail, s"invalid api key", Json.Null))
          }
        }
      }
    }
  }

  def updateTrade = {
    path(JavaUUID) { tradeId =>
      put {
        userFromSession { user =>
          entity(as[TradeRequest]) { tradeRequest =>

            onSuccess( (bittrexService ? UpdateTrade(user, tradeId, tradeRequest))(1.second).mapTo[Option[Trade]] ) {
              case Some(trade) =>
                complete(StatusCodes.OK,  JSendResponse(JsonStatus.Success, "", trade.asJson))
              case None =>
                complete(StatusCodes.Conflict, JSendResponse(JsonStatus.Fail, "can't update trade", Json.Null))
            }
          }
        }
      }
    }
  }
}
