package com.flow.bittrex

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.flow.marketmaker.models.MarketStructures.MarketUpdate
import spray.json.DefaultJsonProtocol

case class BittrexNonce(Nounce: Int, Deltas: List[MarketUpdate])
case class BittrexSummary(H: String, M: String, A: List[BittrexNonce])

case class BittrexGetBalanceResult(Currency: String, Balance: Float, Available: Float, Pending: Float,
                                   CryptoAddress: String)
case class BittrexGetBalanceResponse(success: Boolean, message: String, result: BittrexGetBalanceResult)

// collect your json format instances into a support trait:
trait BittrexJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val update    = jsonFormat13(MarketUpdate)
  implicit val nonce     = jsonFormat2(BittrexNonce)
  implicit val summary   = jsonFormat3(BittrexSummary)

  implicit val getBalanceResult   = jsonFormat5(BittrexGetBalanceResult)
  implicit val getBalanceResponse = jsonFormat3(BittrexGetBalanceResponse)
}

