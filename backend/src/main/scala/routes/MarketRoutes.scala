package routes


import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import cats.instances.uuid
import com.flow.bittrex.api.Bittrex.MarketResult
import com.flow.marketmaker.database.TheEverythingBagelDao
import com.flow.marketmaker.models.{BuyOrder, Trade, TradeRequest}
import com.flow.marketmaker.services.MarketService.{CreateOrder, PostTrade}
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


  // TODO
  // get trades
  // get trade trade by id
  // get single trade
  // when a trade does not execute successfully you need an error log to tell you why
  val marketRoutes = logRequestResult("MarketRoutes") {
    pathPrefix("market") {
      directory ~
      getTrade ~
      listTrades ~
      postTrade ~
      setBuy ~
      setSell
    }
  }

  implicit val encodeTrade: Encoder[Trade] = new Encoder[Trade] {
    final def apply(trade: Trade): Json = {
      val buyPrice = trade.buyPrice match {
        case Some(price) => Json.fromDoubleOrNull(price)
        case None => Json.Null
      }
      val buyTime = trade.buyTime match {
        case Some(time) => Json.fromString(time.toString)
        case None => Json.Null
      }
      val buyCond = trade.buyCondition match {
        case Some(cond) => Json.fromString(cond)
        case None => Json.Null
      }
      val sellPrice = trade.sellPrice match {
        case Some(price) => Json.fromDoubleOrNull(price)
        case None => Json.Null
      }
      val sellTime = trade.sellTime match {
        case Some(time) => Json.fromString(time.toString)
        case None => Json.Null
      }
      val sellCondition = trade.sellCondition match {
        case Some(cond) => Json.fromString(cond)
        case None => Json.Null
      }
      val sellConditions = trade.sellConditions match {
        case Some(conds) => conds
        case None => Json.Null
      }
      Json.obj(
        ("id", Json.fromString(trade.id.toString)),
        ("userId", Json.fromString(trade.userId.toString)),
        ("exchangeName", Json.fromString(trade.exchangeName)),
        ("marketName", Json.fromString(trade.marketName)),
        ("marketCurrencyAbbrev", Json.fromString(trade.marketCurrencyAbbrev)),
        ("marketCurrencyName", Json.fromString(trade.marketCurrencyName)),
        ("baseCurrencyAbbrev", Json.fromString(trade.baseCurrencyAbbrev)),
        ("baseCurrencyName", Json.fromString(trade.baseCurrencyName)),
        ("quantity", Json.fromDoubleOrNull(trade.quantity)),
        ("status", Json.fromString(trade.status.toString)),
        ("createdOn", Json.fromString(trade.createdOn.toString)),
        ("updatedOn", Json.fromString(trade.updatedOn.toString)),
        ("buyTime", buyTime),
        ("buyPrice", buyPrice),
        ("buyCondition", buyCond),
        ("buyCondition", trade.buyConditions),
        ("sellTime", sellTime),
        ("sellPrice", sellPrice),
        ("sellCondition", sellCondition),
        ("sellCondition", sellConditions)
      )
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
        userFromSession { user =>
          onSuccess( bagel.findTradesByUserId(user.id).mapTo[Seq[Trade]] ) {
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
