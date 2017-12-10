package com.flowy.common.api

import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

/**
  * Created by bishop on 9/7/16.
  */
class BittrexClient(implicit context: ExecutionContext, materializer: ActorMaterializer)
  extends BittrexJsonSupport {

  import Bittrex._

  val wsClient = StandaloneAhcWSClient()

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
    * Returns account balance for a single currency
    * @param auth
    * @param currency name of currency. e.g BTC, ETC, ETH...
    * @return BalanceResult wrapped in future
    */
  def accountGetBalance(auth: Auth, currency: String = "BTC"): Future[BalanceResponse] = {
    val endpoint = "account/getbalance"

    val path = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("apikey" -> auth.apiKey)
      .addQueryStringParameters("nonce" -> generateNonce)
      .addQueryStringParameters("currency" -> currency)

    auth.bittrexRequest(path)
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[BalanceResponse]
      }
  }

  /**
    * Returns all currency balances.
    * @param auth
    * @return list of BalanceResult wrapped in future
    */
  def accountGetBalances(auth: Auth): Future[BalancesAuthorization] = {
    val endpoint = "account/getbalances"

    val path = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("apikey" -> auth.apiKey)
      .addQueryStringParameters("nonce" -> generateNonce)

    auth.bittrexRequest(path)
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[BalancesResponse]
      }.map(thing => BalancesAuthorization(auth, thing))
  }

  /**
    * Returns a deposit address for the specified currency.
    * @param auth
    * @param currency name
    * @return future wrapped DepositAddressResult
    */
  def accountGetDepositAddress(auth: Auth, currency: String = "BTC"): Future[DepositAddressResponse] = {
    val endpoint = "account/getdepositaddress"

    val path = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("apikey" -> auth.apiKey)
      .addQueryStringParameters("nonce" -> generateNonce)
      .addQueryStringParameters("currency" -> currency)

    auth.bittrexRequest(path)
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[DepositAddressResponse]
      }
  }

  /**
    * Get order details
    * @param auth
    * @param uuid of order
    * @return future wrapped SingleOrderResponse
    */
  def accountGetOrder(auth: Auth, uuid: String): Future[SingleOrderResponse] = {
    val endpoint = "account/getorder"

    val path = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("apikey" -> auth.apiKey)
      .addQueryStringParameters("nonce" -> generateNonce)
      .addQueryStringParameters("uuid" -> uuid)

    auth.bittrexRequest(path)
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[SingleOrderResponse]
      }
  }

  /**
    * Get account order history
    * @param auth
    * @param market
    * @return future order history response
    */
  def accountGetOrderHistory(auth: Auth, market: Option[String] = None): Future[OrderHistoryResponse] = {
    val endpoint = "account/getorderhistory"

    val path = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("apikey" -> auth.apiKey)
      .addQueryStringParameters("nonce"  -> generateNonce)

    if (market.nonEmpty) {
      path.addQueryStringParameters("market" -> market.get)
    }

    auth.bittrexRequest(path)
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[OrderHistoryResponse]
      }
  }

  /**
    * Get withdrawal history for currency.
    * @param auth
    * @param currency name
    * @return future wrapped WithdrawalHistoryResponse
    */
  def accountGetWithdrawalHistory(auth: Auth, currency: String = "BTC"): Future[HistoryResponse] = {
    val endpoint = "account/getwithdrawalhistory"

    val path = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("apikey" -> auth.apiKey)
      .addQueryStringParameters("nonce" -> generateNonce)
      .addQueryStringParameters("currency" -> currency)

    auth.bittrexRequest(path)
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[HistoryResponse]
      }
  }

  /**
    * Withdraw from exchange.
    * @param auth
    * @param currency name
    * @param quantity
    * @param address to withdraw to
    * @return future wrapped DepositAddressResult
    */
  def accountWithdraw(auth: Auth, currency: String = "BTC", quantity: Double, address: String): Future[UuidResponse] = {
    val endpoint = "account/withdraw"

    val path = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("apikey" -> auth.apiKey)
      .addQueryStringParameters("nonce" -> generateNonce)
      .addQueryStringParameters("currency" -> currency)
      .addQueryStringParameters("quantity" -> quantity.toString)
      .addQueryStringParameters("address" -> address)

    auth.bittrexRequest(path)
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[UuidResponse]
      }
  }

  /**
    * Get deposit history for currency.
    * @param auth
    * @param currency name
    * @return future wrapped HistoryResponse
    */
  def accountGetDepositHistory(auth: Auth, currency: String = "BTC"): Future[DepositResponse] = {
    val endpoint = "account/getdeposithistory"

    val path = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("apikey" -> auth.apiKey)
      .addQueryStringParameters("nonce" -> generateNonce)
      .addQueryStringParameters("currency" -> currency)

    auth.bittrexRequest(path)
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[DepositResponse]
      }
  }


  /*****************************************************************
    * Market API
    ****************************************************************/

  /**
    * Sets a limit buy order
    * @param auth
    * @param market name of market - example: BTC-LTC
    * @param qty
    * @param rate
    * @return a future wrapped order uuid response
    */
  def marketBuyLimit(auth: Auth, market: String, qty: Double, rate: Double): Future[UuidResponse] = {
    val endpoint = "market/buylimit"

    val path = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("apikey" -> auth.apiKey)
      .addQueryStringParameters("nonce"  -> generateNonce)
      .addQueryStringParameters("market" -> market)
      .addQueryStringParameters("quantity" -> qty.toString)
      .addQueryStringParameters("rate" -> rate.toString)

    auth.bittrexRequest(path)
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[UuidResponse]
      }
  }

  /**
    * Set a limit sell order.
    * @param auth
    * @param market name of market - example: BTC-XMR
    * @param qty
    * @param rate
    * @return future wrapped order uuid response
    */
  def marketSellLimit(auth: Auth, market: String, qty: Double, rate: Double): Future[UuidResponse] = {
    val endpoint = "market/selllimit"

    val path = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("apikey" -> auth.apiKey)
      .addQueryStringParameters("nonce"  -> generateNonce)
      .addQueryStringParameters("market" -> market)
      .addQueryStringParameters("quantity" -> qty.toString)
      .addQueryStringParameters("rate" -> rate.toString)

    auth.bittrexRequest(path)
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[UuidResponse]
      }
  }

  /**
    * Cancels an open order.
    * @param auth
    * @param uuid
    * @return future wrapped null response
    */
  def marketCancel(auth: Auth, uuid: String): Future[StandardNullResponse] = {
    val endpoint = "market/cancel"

    val path = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("apikey" -> auth.apiKey)
      .addQueryStringParameters("nonce"  -> generateNonce)
      .addQueryStringParameters("uuid" -> uuid)

    auth.bittrexRequest(path)
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[StandardNullResponse]
      }
  }

  /**
    * Get open orders.
    * @param auth
    * @param market option name of the market
    * @return future wrapped GetOpenOrdersResponse
    */
  def marketGetOpenOrders(auth: Auth, market: Option[String] = None): Future[GetOpenOrdersResponse] = {
    val endpoint = "market/getopenorders"

    val path = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("apikey" -> auth.apiKey)
      .addQueryStringParameters("nonce"  -> generateNonce)

    if (market.nonEmpty) {
      path.addQueryStringParameters("market" -> market.get)
    }

    auth.bittrexRequest(path)
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[GetOpenOrdersResponse]
      }
  }

  /*****************************************************************
    * PUBLIC API
    ****************************************************************/

  /**
    * Used to get the open and available trading markets at Bittrex along with other meta data.
    * @return future wrapped MarketResponse
    */
  def publicGetMarkets(): Future[MarketResponse] = {
    val endpoint = "public/getmarkets"
    val request = wsClient.url(s"$base_url/$endpoint")
    request
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[MarketResponse]
      }
  }

  /**
    * Used to get all supported currencies at Bittrex along with other meta data.
    * @return future wrapped CurrencyResponse
    */
  def publicGetCurrencies(): Future[CurrencyResponse] = {
    val endpoint = "public/getcurrencies"
    val request = wsClient.url(s"$base_url/$endpoint")
    request
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[CurrencyResponse]
      }
  }

  /**
    * Used to get the current tick values for a market.
    * @param market name of market (BTC-XMR)
    * @return future wrapped TickerResponse
    */
  def publicGetTicker(market: String): Future[TickerResponse] = {
    val endpoint = "public/getticker"
    val request = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("market" -> market)

    request
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[TickerResponse]
      }
  }

  /**
    * Used to get the last 24 hour summary of all active exchanges
    * @return future wrapped MarketSummaryResponse
    */
  def publicGetMarketSummaries(): Future[MarketSummaryResponse] = {
    val endpoint = "public/getmarketsummaries"
    val request = wsClient.url(s"$base_url/$endpoint")

    request
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[MarketSummaryResponse]
      }
  }

  /**
    * Used to get the last 24 hour summary of a single market
    * @param market name of market (BTC-LTC)
    * @return future wrapped MarketSummaryResponse
    */
  def publicGetMarketSummary(market: String): Future[MarketSummaryResponse] = {
    val endpoint = "public/getmarketsummary"
    val request = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("market" -> market)

    request
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[MarketSummaryResponse]
      }
  }

  /**
    * Used to retrieve the orderbook for a given market
    * @param market name of market (BTC-LTC)
    * @param side is either 'both', 'buy', or 'sell'
    * @return future wrapped OrderBookResponse
    */
  def publicGetOrderBook(market: String, side: String): Future[OrderBookResponse] = {
    val endpoint = "public/getorderbook"
    val request = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("market" -> market)
      .addQueryStringParameters("type" -> side)

    request
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[OrderBookResponse]
      }
  }

  /**
    * Used to retrieve the latest trades that have occured for a specific market.
    * @param market name of market (BTC-LTC)
    * @return future wrapped OrderBookResponse
    */
  def publicGetMarketHistory(market: String): Future[MarketHistoryResponse] = {
    val endpoint = "public/getmarkethistory"
    val request = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("market" -> market)

    request
      .get()
      .flatMap { response =>
        Unmarshal(response.body).to[MarketHistoryResponse]
      }
  }

}

