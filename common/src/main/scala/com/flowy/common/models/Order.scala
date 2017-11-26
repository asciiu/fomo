package com.flowy.common.models

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.semiauto._


object Exchange extends Enumeration {
  val Bittrex   = Value("bittrex")
  val GDax      = Value("gdax")
  val Poloniex  = Value("poloniex")
}

object ApiKeyStatus extends Enumeration {
  val Added      = Value("added")
  val Verified   = Value("verified")
  val Invalid    = Value("invalid")
}

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
                 buyConditions: String,
                 sellConditions: Option[String]) {
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
                 buyCondition: Option[String],
                 buyConditions: String,
                 sellTime: Option[OffsetDateTime],
                 sellPrice: Option[Double],
                 sellCondition: Option[String],
                 sellConditions: Option[String])

object Trade {

  implicit val condEcoder: Encoder[Condition] = deriveEncoder[Condition]
  implicit val condAEcoder: Encoder[ConditionArray] = deriveEncoder[ConditionArray]

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
      val buyCondition = trade.buyCondition match {
        case Some(cond) => Json.fromString(cond.toString)
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
        case Some(cond) => Json.fromString(cond.toString)
        case None => Json.Null
      }
      val sellConditions = trade.sellConditions match {
        case Some(conds) => Json.fromString(conds.toString)
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
        ("buyCondition", buyCondition),
        ("buyConditions", Json.fromString(trade.buyConditions)),
        ("sellTime", sellTime),
        ("sellPrice", sellPrice),
        ("sellCondition", sellCondition),
        ("sellConditions", sellConditions)
      )
    }
  }

  implicit val decodeTrade: Decoder[Trade] = new Decoder[Trade] {
    final def apply(c: HCursor): Decoder.Result[Trade] =
      for {
        id <- c.downField("id").as[String]
        userId <- c.downField("userId").as[String]
        exchangeName <- c.downField("exchangeName").as[String]
        marketName <- c.downField("marketName").as[String]
        marketCurrencyAbbrev <- c.downField("marketCurrencyAbbrev").as[String]
        marketCurrencyName <- c.downField("marketCurrencyName").as[String]
        baseCurrencyAbbrev <- c.downField("baseCurrencyAbbrev").as[String]
        baseCurrencyName <- c.downField("baseCurrencyName").as[String]
        quantity <- c.downField("quantity").as[Double]
        status <- c.downField("status").as[String]
        createdOn <- c.downField("createdOn").as[String]
        updatedOn <- c.downField("updatedOn").as[String]
        buyConditions <- c.downField("buyConditions").as[String]
        sellConditions <- c.downField("sellConditions").as[String]
      } yield {
        new Trade(UUID.fromString(id),
          UUID.fromString(userId),
          exchangeName,
          marketName,
          marketCurrencyAbbrev,
          marketCurrencyName,
          baseCurrencyAbbrev,
          baseCurrencyName,
          quantity,
          TradeStatus.withName(status),
          OffsetDateTime.parse(createdOn),
          OffsetDateTime.parse(updatedOn),
          None,
          None,
          None,
          buyConditions,
          None,
          None,
          None,
          Some(sellConditions))
      }
  }

  def fromRequest(tradeRequest: TradeRequest, forUserId: UUID): Trade = {
    val tradeId = UUID.randomUUID()
    val now = Instant.now().atOffset(ZoneOffset.UTC)

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
      tradeRequest.buyConditions,
      None,
      None,
      None,
      tradeRequest.sellConditions
    )
  }
}
