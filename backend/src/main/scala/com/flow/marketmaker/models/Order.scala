package com.flow.marketmaker.models

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

import spray.json._
import DefaultJsonProtocol._


object OrderStatus extends Enumeration {
  val Pending    = Value("pending")
  val Completed  = Value("completed")
}


object OrderType extends Enumeration {
  val Buy = Value("buy")
  val Sell    = Value("sell")
}


// this gets posted from the client
// move this to the route file
case class BuyCondition(conditionType: String, indicator: String, operator: String, value: Double)
case class BuyOrder(exchangeName: String,
                    marketName: String,
                    quantity: Double,
                    buyConditions: List[BuyCondition])


// TODO you need to remove the classes above
//case class Condition(conditionType: String, indicator: String, operator: String, value: Double)
//case class Order(exchangeName: String,
//                 marketName: String,
//                 quantity: Double,
//                 conditions: List[BuyCondition])

object TradeStatus extends Enumeration {
  val Pending    = Value("pending")
  val Bought     = Value("bought")
  val Sold       = Value("sold")
  val Cancelled  = Value("cancelled")
}

case class Condition(conditionType: String, indicator: String, operator: String, value: Double, description: Option[String])
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

  implicit val condFormat = jsonFormat5(Condition)
  implicit val orderFormat = jsonFormat2(ConditionArray)

//  def buyOrder(userId: UUID): Order = {
//    Order(UUID.randomUUID(), userId, exchangeName, marketName,
//      Instant.now().atOffset(ZoneOffset.UTC), None, None, None, quantity,
//      OrderType.Buy, OrderStatus.Pending, buyConditions.toJson)
//  }
//
//  def sellOrder(userId: UUID): Option[Order] = {
//    sellConditions match {
//      case Some(cond) =>
//        Some( Order (UUID.randomUUID (), userId, exchangeName, marketName,
//          Instant.now ().atOffset (ZoneOffset.UTC), None, None, None, quantity,
//          OrderType.Sell, OrderStatus.Pending, cond.toJson) )
//      case None =>
//        None
//    }
//  }
}

case class TradeResponse(id: UUID,
                        exchangeName: String,
                        marketName: String,
                        marketCurrency: String,
                        marketCurrencyLong: String,
                        baseCurrency: String,
                        baseCurrencyLong: String,
                        createdTime: OffsetDateTime,
                        quantity: Double,
                        boughtTime: Option[OffsetDateTime],
                        boughtPrice: Option[Double],
                        soldTime: Option[OffsetDateTime],
                        soldPrice: Option[Double],
                        status: TradeStatus.Value,
                        buyConditions: String,
                        sellConditions: String)


case class Order(id: UUID,
                 userId: UUID,
                 exchangeName: String,
                 marketName: String,
                 createdTime: OffsetDateTime,
                 completedTime: Option[OffsetDateTime] = None,
                 completedCondition: Option[String] = None,
                 priceActual: Option[Double] = None,
                 quantity: Double,
                 orderType: OrderType.Value,
                 status: OrderStatus.Value,
                 conditions: JsValue) {

  def conditionsToArray: JsArray = conditions.asInstanceOf[JsArray]
  // evaluates all conditions and returns true if any of the conditions are true
  //def isCondition(test: Any): Boolean = conditions.exists( _.evaluate(test) )
  //def getCondition(test: Any): Option[String] = conditions.find( _.evaluate(test) ).map (_.toString)
}

object Order {

  implicit val buyFormat = jsonFormat4(BuyCondition)

  def fromBuyOrder(buyOrder: BuyOrder, forUserId: UUID): Order = {

//    val conditions = buyOrder.buyConditions.map { c =>
//
//      c.conditionType match {
//        case "simpleConditional" => SimpleConditionalFactory.makeCondition(c.operator, c.value)
//        case _ => NullCondition
//      }
//    }.asInstanceOf[List[Conditional]]

    //Order(None, forUserId, buyOrder.exchangeName, buyOrder.marketName,
    //  Instant.now().atOffset(ZoneOffset.UTC), None, None, None, buyOrder.quantity,
    //  OrderType.Buy, OrderStatus.Pending, buyOrder.buyConditions)

    Order(UUID.randomUUID(), forUserId, buyOrder.exchangeName, buyOrder.marketName,
      Instant.now().atOffset(ZoneOffset.UTC), None, None, None, buyOrder.quantity,
      OrderType.Buy, OrderStatus.Pending, buyOrder.buyConditions.toJson)
  }
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
                 buyConditions: JsValue,
                 sellTime: Option[OffsetDateTime],
                 sellPrice: Option[Double],
                 sellCondition: Option[String],
                 sellConditions: Option[JsValue])

object Trade {

  implicit val condFormat = jsonFormat5(Condition)
  implicit val orderFormat = jsonFormat2(ConditionArray)

  def fromRequest(tradeRequest: TradeRequest, forUserId: UUID): Trade = {
    val uuid = UUID.randomUUID()
    val now = Instant.now().atOffset(ZoneOffset.UTC)
    val sellConditions = tradeRequest.sellConditions match {
      case Some(conditions) => conditions.toJson
      case None => JsNull
    }

    Trade(uuid,
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
      tradeRequest.buyConditions.toJson,
      None,
      None,
      None,
      Some(sellConditions)
    )
  }
}
