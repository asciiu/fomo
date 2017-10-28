package com.flow.marketmaker.models

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID


case class BuyCondition(conditionType: String, indicator: String, operator: String, value: Double)


/*
id: "1",
exchange: "bittrex",
exchangeName: "bittrex",
marketName: "BAT-BTC",
marketCurrency: "BAT",
marketCurrencyLong: "Basic Attention Token",
baseCurrency: "BTC",
baseCurrencyLong: "Bitcoin",
createdTime: "2014-07-12T03:41:25.323",
boughtTime:"2014-07-12T04:42:25.323",
quantity: 1000,
boughtPriceAsked: 0.000045,
boughtPriceActual: 0.000044,
//soldTime: "",
//soldPriceAsked: 0.00005,
//soldPriceActual: 0.000051,
status: "bought",
buyConditions:
*/

object OrderStatus extends Enumeration {
  val Pending    = Value("pending")
  val Completed  = Value("completed")
}

case class BuyOrder(exchangeName: String,
                    marketName: String,
                    quantity: Double,
                    buyConditions: List[BuyCondition])

case class Order(id: Option[Long] = None,
                 userId: UUID,
                 exchangeName: String,
                 marketName: String,
                 marketCurrency: String,
                 marketCurrencyLongName: String,
                 createdTime: OffsetDateTime,
                 completedTime: Option[OffsetDateTime] = None,
                 completedCondition: Option[String] = None,
                 priceActual: Option[Double] = None,
                 quantity: Double,
                 orderType: OrderType.Value,
                 status: OrderStatus.Value,
                 orConditions: List[TradeCondition]) {

  // evaluates all conditions and returns true if any of the conditions are true
  def isCondition(test: Any): Boolean = orConditions.exists( _.evaluate(test) )
  def getCondition(test: Any): Option[String] = orConditions.find( _.evaluate(test) ).map (_.toString)
}

object Order {
  def fromBuyOrder(buyOrder: BuyOrder, forUserId: UUID): Order = {

    val conditions = buyOrder.buyConditions.map{ c => SimpleConditionalFactory.makeCondition(c.operator, c.value) }

    Order(None, forUserId, buyOrder.exchangeName, buyOrder.marketName, buyOrder.marketName.split("-")(1), "",
      Instant.now().atOffset(ZoneOffset.UTC), None, None, None, buyOrder.quantity,
      OrderType.Buy, OrderStatus.Pending, conditions)
  }
}

object OrderType extends Enumeration {
  val Buy = Value("buy")
  val Sell    = Value("sell")
}


case class TradeOrder(val id: UUID,
                      val userId: UUID,
                      val exchangeName: String,
                      val marketName: String,
                      val currencyName: String,
                      val side: OrderType.Value,
                      val quantity: Double,
                      orConditions: List[TradeCondition]
                     ) {

  val createdAt = Instant.now().atOffset(ZoneOffset.UTC)

  // evaluates all conditions and returns true if any of the conditions are true
  def evaluate(test: Any): Boolean = orConditions.exists( _.evaluate(test) )
}

object TradeOrder {

  def apply(userId: UUID, exchangeName: String, marketName: String, currencyName: String,
            side: OrderType.Value, quantity: Double, orConditions: List[TradeCondition]): TradeOrder =
    TradeOrder(UUID.randomUUID(), userId, exchangeName, marketName,
      currencyName, side, quantity, orConditions)
}

abstract class TradeCondition() {
  def symbol: String

  def evaluate(test: Any): Boolean
}

object Indicator extends Enumeration {
  val Price = Value("price")
}

case class NullCondition(val basePrice: Double = 0.0) extends TradeCondition  {
  lazy val symbol = "null"
  def evaluate(testPrice: Any) = false
}

case class GreaterThanEq(basePrice: Double) extends TradeCondition {
  lazy val symbol = ">="
  lazy val indicator = Indicator.Price

  def evaluate(testPrice: Any) = testPrice.asInstanceOf[Double] >= basePrice
  override def toString = s">= $basePrice"
}

case class LessThanEq(basePrice: Double) extends TradeCondition {
  lazy val symbol = "<="
  lazy val indicator = Indicator.Price

  def evaluate(testPrice: Any) = testPrice.asInstanceOf[Double] <= basePrice

  override def toString = s">= $basePrice"
}

object SimpleConditionalFactory {
  def makeCondition(str: String, value: Double) = {
    str match {
      case "<=" => LessThanEq(value)
      case ">=" => GreaterThanEq(value)
    }
  }
}
