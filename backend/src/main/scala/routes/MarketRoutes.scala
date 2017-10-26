package routes

import java.util.UUID
import javax.ws.rs.{GET, POST, Path}

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.flow.bittrex.database.TradeDao
import com.flow.bittrex.models.BittrexTrade
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
  def tradeDao: TradeDao

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

            // TODO a trade order has a created at time
            // it also contains isOpen
            // closed_at timestamp
//            exchange: "bittrex",
//            exchangeName: "bittrex",
//            marketName: "BAT-BTC",
//            marketCurrency: "BAT",
//            marketCurrencyLong: "Basic Attention Token",
//            baseCurrency: "BTC",
//            baseCurrencyLong: "Bitcoin",
//            createdTime: "2014-07-12T03:41:25.323",
//            //boughtTime:"2014-07-12T04:42:25.323",
//            quantity: 1000,
//            //boughtPriceAsked: 0.000045,
//            //boughtPriceActual: 0.000044,
//            //soldTime: "",
//            //soldPriceAsked: 0.00005,
//            //soldPriceActual: 0.000051,
//            status: "set",


            val order = new TradeOrder(userId = user.id,
              exchangeName = in.exchangeName,
              marketName = in.marketName,
              currencyName = in.marketName.split("-")(1),
              side = TradeType.Buy,
              quantity = in.quantity,
              orConditions = conditions
            )

            // TODO this needs to be merged with the TradeOrder concept
            tradeDao.insert(BittrexTrade.withRandomUUID(in.marketName, true, in.quantity, 0.0))

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
