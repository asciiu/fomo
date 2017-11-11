package com.flow.bittrex

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.flowy.marketmaker.models.MarketStructures.MarketUpdate
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat}
import spray.json._

import scala.util.Try

case class BittrexNonce(Nounce: Int, Deltas: List[MarketUpdate])
case class BittrexSummary(H: String, M: String, A: List[BittrexNonce])

case class BittrexGetBalanceResult(Currency: String, Balance: Float, Available: Float, Pending: Float,
                                   CryptoAddress: String)
case class BittrexGetBalanceResponse(success: Boolean, message: String, result: BittrexGetBalanceResult)

// collect your json format instances into a support trait:
trait BittrexJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val offsetDateTimeFormat = new JsonFormat[OffsetDateTime] {
    override def write(obj: OffsetDateTime): JsValue = JsString(formatter.format(obj))

    override def read(json: JsValue): OffsetDateTime = {
      json match {
        case JsString(lTString) =>
          Try(LocalDateTime.parse(lTString, formatter).atOffset(ZoneOffset.UTC)).getOrElse(deserializationError(deserializationErrorMessage))
        case _ => deserializationError(deserializationErrorMessage)
      }
    }

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val deserializationErrorMessage =
      s"Expected date time in ISO offset date time format ex. ${OffsetDateTime.now().format(formatter)}"
  }

  implicit val update    = jsonFormat14(MarketUpdate)
  implicit val nonce     = jsonFormat2(BittrexNonce)
  implicit val summary   = jsonFormat3(BittrexSummary)

  implicit val getBalanceResult   = jsonFormat5(BittrexGetBalanceResult)
  implicit val getBalanceResponse = jsonFormat3(BittrexGetBalanceResponse)
}

