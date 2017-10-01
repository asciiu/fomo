package com.flow.bittrex

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.flow.marketmaker.models.MarketStructures.MarketUpdate
import spray.json.DefaultJsonProtocol

case class BittrexNonce(Nounce: Int, Deltas: List[MarketUpdate])
case class BittrexSummary(val H: String, val M: String, val A: List[BittrexNonce])

// collect your json format instances into a support trait:
trait BittrexJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val update    = jsonFormat13(MarketUpdate)
  implicit val nonce     = jsonFormat2(BittrexNonce)
  implicit val summary   = jsonFormat3(BittrexSummary)
}

