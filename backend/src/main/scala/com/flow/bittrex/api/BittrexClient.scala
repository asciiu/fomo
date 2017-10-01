package com.flow.bittrex.api

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
                          (implicit context: ExecutionContext) extends Auth(apiKey, secretKey) {

  //import PoloniexTradeClient._
  //val url = conf.getString("poloniex.url.trade").getOrElse("https://poloniex.com/tradingApi")
  val base_url = "https://bittrex.com/api/v1.1"

  private def nonce = System.currentTimeMillis().toInt.toString
  //private def params(command: String, nonce: String) = s"command=$command&nonce=$nonce"

  /**
    * Returns all of your available balances.
    */
  def availableBalances(): Unit = {
    val endpoint = "account/getbalances"

    //val n = nonce
    //val command = "returnBalances"

    val path = wsClient.url(s"$base_url/$endpoint")
      .addQueryStringParameters("apikey" -> apiKey)
      .addQueryStringParameters("nonce" -> generateNonce)

    setAuthenticationHeaders(path)
      .get().map(response => println(response.body))
    //    .addQueryStringParameters("apiKey" -> apiKey)
    //    .get()
    //  .map{ response => ???
    //}
  }

  import java.io.UnsupportedEncodingException
  import java.security.NoSuchAlgorithmException
  import java.security.SecureRandom
  import java.util.Base64

  def generateNonce: String = {
    val random = SecureRandom.getInstance("SHA1PRNG")
    random.setSeed(System.currentTimeMillis)

    val nonceBytes = new Array[Byte](16)
    random.nextBytes(nonceBytes)
    new String(Base64.getEncoder.encode(nonceBytes), "UTF-8")
  }

//  private val balanceReads: Reads[(BigDecimal, BigDecimal, BigDecimal)] = (
//    (JsPath \ "available").read[BigDecimal] and
//      (JsPath \ "onOrders").read[BigDecimal] and
//      (JsPath \ "btcValue").read[BigDecimal]
//      tupled
//    )
//
//  private implicit val marketRead: Reads[List[CurrencyBalance]] = Reads( js =>
//    JsSuccess(js.as[JsObject].fieldSet.map { currency =>
//      val balance = currency._2.as[(BigDecimal, BigDecimal, BigDecimal)](balanceReads)
//      CurrencyBalance(currency._1, balance._1, balance._2, balance._3)
//    }.toList))
//
//  /**
//    * Returns all of your balances, including available balance, balance on orders,
//    * and the estimated BTC value of your balance.
//    * {"LTC":{"available":"5.015","onOrders":"1.0025","btcValue":"0.078"},"NXT:{...} ... }
//    */
//  def completeBalances(): Future[List[CurrencyBalance]] = {
//    val n = nonce
//    val command = "returnCompleteBalances"
//
//    setAuthenticationHeaders(wsClient.url(url), params(command, n))
//      .post(Map("command" -> Seq(command), "nonce" -> Seq(n)))
//      .map{ response =>
//        response.json.validate[List[CurrencyBalance]] match {
//          case s: JsSuccess[List[CurrencyBalance]] => s.get
//          case e: JsError => List[CurrencyBalance]()
//        }
//      }
//  }
//
//  val strToLong = Reads[Long](js =>
//    js.validate[String].map[Long] { num =>
//      num.toLong
//    })
//
//  private val openOrderRead: Reads[Order] = (
//      (JsPath \ "orderNumber").read[Long](strToLong) and
//      (JsPath \ "type").read[String] and
//      (JsPath \ "rate").read[BigDecimal] and
//      (JsPath \ "startingAmount").read[BigDecimal] and
//      (JsPath \ "amount").read[BigDecimal] and
//      (JsPath \ "total").read[BigDecimal]
//    ) (Order.apply _)
//
//  private implicit val openOrderListRead: Reads[List[OrdersOpened]] = Reads( js =>
//    JsSuccess(js.as[JsObject].fieldSet.map { market =>
//      val deets = market._2.as[JsArray].value.map(j => j.validate[Order](openOrderRead).get).toList
//      OrdersOpened(market._1, deets)
//    }.toList))
//
//
//  /**
//    * Returns your open orders for a given market
//    * {"BTC_1CR":[],"BTC_AC":[{"orderNumber":"120466","type":"sell","rate":"0.025","amount":"100","total":"2.5"},
//    * {"orderNumber":"120467","type":"sell","rate":"0.04","amount":"100","total":"4"}], ... }
//    */
//  def openOrders(): Future[List[OrdersOpened]] = {
//    val n = nonce
//    val command = "returnOpenOrders"
//    val postParams = params(command, n) + "&currencyPair=all"
//
//    setAuthenticationHeaders(wsClient.url(url), postParams)
//      .post(Map("command" -> Seq(command), "nonce" -> Seq(n), "currencyPair" -> Seq("all")))
//      .map{ response =>
//
//        response.json.validate[List[OrdersOpened]] match {
//          case s: JsSuccess[List[OrdersOpened]] =>
//            s.get.filter( o => !o.orders.isEmpty)
//          case e: JsError =>
//            List[OrdersOpened]()
//        }
//      }
//  }
//
//  /**
//    * Returns all trades involving a given order, specified by the "orderNumber" POST parameter.
//    * If no trades for the order have occurred or you specify an order that does not belong to you,
//    * you will receive an error.
//    *
//    * @param orderNumber
//    */
//  def orderTrades(orderNumber: Int): Unit = {
//    val n = nonce
//    val command = "returnOrderTrades"
//    val postParams = params(command, n) + s"&orderNumber=${orderNumber}"
//
//    setAuthenticationHeaders(wsClient.url(url), postParams)
//      .post(Map("command" -> Seq(command), "nonce" -> Seq(n), "orderNumber" -> Seq(orderNumber.toString)))
//      .map{ response =>
//        println(s"HERE ${response.json.toString()}")
//      }
//  }
//
//  implicit val tradeRead = Json.format[Trade]
//  private implicit val buyOrderRead: Reads[OrderNumber] = (
//    (JsPath \ "orderNumber").read[Long](strToLong) and
//      (JsPath \ "resultingTrades").read[List[Trade]]
//    )(OrderNumber.apply _)
//
//  /**
//    * Places a limit buy order in a given market. If successful, the method will return the order number
//    *
//    * @param side - buy or sell
//    * @param currencyPair
//    * @param rate
//    * @param amount
//    */
//  def placeOrder(side: String, currencyPair: String, rate: BigDecimal, amount: BigDecimal): Future[Option[OrderNumber]] = {
//    val n = nonce
//    val command = side
//    val rt = rate.setScale(8)
//    val am = amount.setScale(8)
//    val postParams = s"rate=$rt&currencyPair=$currencyPair&nonce=$n&command=$command&amount=$am"
//
//    setAuthenticationHeaders(wsClient.url(url), postParams)
//      .post(Map("command" -> Seq(command),
//      "nonce" -> Seq(n),
//      "currencyPair" -> Seq(currencyPair),
//      "rate" -> Seq(rt.toString),
//      "amount" -> Seq(am.toString)
//      ))
//      .map{ response =>
//        response.json.validate[OrderNumber] match {
//          case s: JsSuccess[OrderNumber] => Some(s.get)
//          case e: JsError => None
//        }
//      }
//  }
//
//  case class Status(success: Int)
//  implicit val statusRead = Json.format[Status]
//
//  /**
//    * Cancel an open order.
//    * @param orderNumber
//    * @return
//    */
//  def cancelOrder(orderNumber: Long): Future[Boolean] = {
//    val n = nonce
//    val command = "cancelOrder"
//    val postParams = params(command, n) + s"&orderNumber=$orderNumber"
//
//    setAuthenticationHeaders(wsClient.url(url), postParams)
//      .post(Map("command" -> Seq(command),
//        "nonce" -> Seq(n),
//        "orderNumber" -> Seq(orderNumber.toString)
//      ))
//      .map{ response =>
//        response.json.validate[Status] match {
//          case s: JsSuccess[Status] if s.get.success > 0 => true
//          case e: JsError => false
//          case _ => false
//        }
//      }
//  }
//
//  private implicit val moveOrderRead: Reads[MoveOrderStatus] = (
//    (JsPath \ "success").read[Int] and
//    (JsPath \ "orderNumber").read[Long](strToLong)
//    )(MoveOrderStatus.apply _)
//  /**
//    * Cancels an order and places a new one of the same type in a single atomic transaction,
//    * meaning either both operations will succeed or both will fail.
//    *
//    * @param orderNumber
//    * @param rate
//    * @param amount
//    */
//  def moveOrder(orderNumber: Long, rate: BigDecimal, amount: BigDecimal): Future[MoveOrderStatus] = {
//    val n = nonce
//    val command = "moveOrder"
//    val postParams = s"rate=$rate&orderNumber=$orderNumber&nonce=$n&command=$command&amount=$amount"
//
//    // TODO add resulting trades in the validation read
//    //{"success":1,"orderNumber":"3254037865","resultingTrades":{"BTC_AMP":[]}}
//    setAuthenticationHeaders(wsClient.url(url), postParams)
//      .post(Map("command" -> Seq(command),
//        "nonce" -> Seq(n),
//        "orderNumber" -> Seq(orderNumber.toString),
//        "rate" -> Seq(rate.toString),
//        "amount" -> Seq(amount.toString)
//      ))
//      .map{ response =>
//        response.json.validate[MoveOrderStatus] match {
//          case s: JsSuccess[MoveOrderStatus] => s.get
//          case e: JsError => MoveOrderStatus(0, 0)
//        }
//      }
//  }
}
