package com.flow.bittrex.api

import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.flow.bittrex.{BittrexGetBalanceResponse, BittrexJsonSupport, BittrexSummary}
import com.typesafe.config.Config
import play.api.libs.ws.StandaloneWSClient

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

object BittrexClient {
  case class CurrencyBalance(marketName: String,
                             available: BigDecimal,
                             onOrders: BigDecimal,
                             btcValue: BigDecimal)

  case class MoveOrderStatus(status: Int, orderNumber: Long)

  case class Order(orderNumber: Long,
                   side: String,
                   rate: BigDecimal,
                   startingAmount: BigDecimal,
                   amount: BigDecimal,
                   total: BigDecimal)

  //case class OrderNumber(orderNumber: Long,
   //                      resultingTrades: List[Trade])

  case class OrdersOpened(marketName: String, orders: List[Order])

  //case class Trade(date: DateTime,
  //                 amount: BigDecimal,
  //                 rate: BigDecimal,
  //                 total: BigDecimal,
  //                 tradeId: Long,
  //                 `type`: String)
}

/**
  * Created by bishop on 9/7/16.
  */
class BittrexClient (apiKey: String,
                           secretKey: String,
                           wsClient: StandaloneWSClient)
                          (implicit context: ExecutionContext,
                           materializer: ActorMaterializer)
  extends Auth(apiKey, secretKey) with BittrexJsonSupport{

  // TODO read this from config?
  val base_url = "https://bittrex.com/api/v1.1"

  import java.security.SecureRandom
  import java.util.Base64

  private def generateNonce: String = {
    val random = SecureRandom.getInstance("SHA1PRNG")
    random.setSeed(System.currentTimeMillis)

    val nonceBytes = new Array[Byte](16)
    random.nextBytes(nonceBytes)
    new String(Base64.getEncoder.encode(nonceBytes), "UTF-8")
  }

  /*****************************************************************
    * ACCOUNT API
    ****************************************************************/

  /**
    * Returns all of your available balances.
    */
  def accountGetBalances(): Unit = {
    val endpoint = "account/getbalances"

    val path = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("apikey" -> apiKey)
      .addQueryStringParameters("nonce" -> generateNonce)

    setAuthenticationHeaders(path)
      .get().map(
      // TODO deserialize
      response => println(response.body))
  }

  def accountGetBalance(currency: String = "BTC"): Unit = {
    val endpoint = "account/getbalance"

    val path = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("apikey" -> apiKey)
      .addQueryStringParameters("nonce" -> generateNonce)
      .addQueryStringParameters("currency" -> currency)

    setAuthenticationHeaders(path)
      .get().map{ response =>
        Unmarshal(response.body).to[BittrexGetBalanceResponse].map { balance  =>
          if (balance.success) {
            println(balance.result)
          }
        }
      }
  }
}
