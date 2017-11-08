package com.flow.marketmaker.models

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID
import io.circe.{Encoder, Json}
import io.circe.syntax._
import io.circe.generic.semiauto._
import spray.json._
import DefaultJsonProtocol._


object TradeStatus extends Enumeration {
  val Pending    = Value("pending")
  val Bought     = Value("bought")
  val Sold       = Value("sold")
  val Cancelled  = Value("cancelled")
}

case class Condition(id: Option[UUID], conditionType: String, indicator: String, operator: String, value: Double, description: Option[String])
case class ConditionArray(collectionType: String, conditions: List[Condition])
case class TradeRequest(
                 exchangeName: String,
                 marketName: String,
                 marketCurrencyAbbrev: Option[String],
                 marketCurrencyName: Option[String],
                 baseCurrencyAbbrev: Option[String],
                 baseCurrencyName: Option[String],
                 quantity: Double,
                 buyConditions: ConditionArray,
                 sellConditions: Option[ConditionArray]) {
}

case class Trade(id: UUID,
                 userId: UUID,
                 exchangeName: String,
                 marketName: String,
                 marketCurrencyAbbrev: String,
                 marketCurrencyName: String,
                 baseCurrencyAbbrev: String,
                 baseCurrencyName: String,
                 quantity: Double,
                 status: TradeStatus.Value,
                 createdOn: OffsetDateTime,
                 updatedOn: OffsetDateTime,
                 buyTime: Option[OffsetDateTime],
                 buyPrice: Option[Double],
                 buyConditionId: Option[UUID],
                 buyConditions: Json,
                 sellTime: Option[OffsetDateTime],
                 sellPrice: Option[Double],
                 sellConditionId: Option[UUID],
                 sellConditions: Option[Json])

object Trade {

  implicit val condEcoder: Encoder[Condition] = deriveEncoder[Condition]
  implicit val condAEcoder: Encoder[ConditionArray] = deriveEncoder[ConditionArray]

  def fromRequest(tradeRequest: TradeRequest, forUserId: UUID): Trade = {
    val tradeId = UUID.randomUUID()
    val now = Instant.now().atOffset(ZoneOffset.UTC)
    val sellConditions = tradeRequest.sellConditions match {
      case Some(conditions) => conditions.asJson
      case None => Json.Null
    }

    Trade(tradeId,
      forUserId,
      tradeRequest.exchangeName,
      tradeRequest.marketName,
      tradeRequest.marketCurrencyAbbrev.getOrElse(""),
      tradeRequest.marketCurrencyName.getOrElse(""),
      tradeRequest.baseCurrencyAbbrev.getOrElse(""),
      tradeRequest.baseCurrencyName.getOrElse(""),
      tradeRequest.quantity,
      TradeStatus.Pending,
      now,
      now,
      None,
      None,
      None,
      tradeRequest.buyConditions.asJson,
      None,
      None,
      None,
      Some(sellConditions)
    )
  }
}
