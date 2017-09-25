package com.flow.bittrex

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class BittrexMarketUpdate(MarketName: String, High: Float, Low: Float, Volume: Float,
                               Last: Float, BaseVolume: Float, TimeStamp: String, Bid: Float,
                               Ask: Float, OpenBuyOrders: Int, OpenSellOrders: Int, PrevDay: Float,
                               Created: String)
case class BittrexNonce(Nounce: Int, Deltas: List[BittrexMarketUpdate])
case class BittrexSummary(val H: String, val M: String, val A: List[BittrexNonce])

// collect your json format instances into a support trait:
trait BittrexJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val update    = jsonFormat13(BittrexMarketUpdate)
  implicit val nonce     = jsonFormat2(BittrexNonce)
  implicit val summary   = jsonFormat3(BittrexSummary)
}

