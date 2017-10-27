package com.flow.marketmaker.models

import java.time.{Instant, ZoneOffset}
import java.util.UUID

case class BuyCondition(conditionType: String, indicator: String, operator: String, value: Double)

case class BuyOrder(exchangeName: String,
                    marketName: String,
                    quantity: Double,
                    buyConditions: List[BuyCondition])

object TradeType extends Enumeration {
  val Buy = Value("buy")
  val Sell    = Value("sell")
}


case class TradeOrder(val id: UUID,
                      val userId: UUID,
                      val exchangeName: String,
                      val marketName: String,
                      val currencyName: String,
                      val side: TradeType.Value,
                      val quantity: Double,
                      orConditions: List[TradeCondition]
                     ) {

  val createdAt = Instant.now().atOffset(ZoneOffset.UTC)

  //boughtTime:"2014-07-12T04:42:25.323",
  //quantity: 1000,
  //boughtPriceAsked: 0.000045,
  //boughtPriceActual: 0.000044,
  //soldTime: "",
  //soldPriceAsked: 0.00005,
  //soldPriceActual: 0.000051,
  //status: "set",

  // evaluates all conditions and returns true if any of the conditions are true
  def evaluate(test: Any): Boolean = orConditions.exists( _.evaluate(test) )
}

object TradeOrder {

  def apply(userId: UUID, exchangeName: String, marketName: String, currencyName: String,
            side: TradeType.Value, quantity: Double, orConditions: List[TradeCondition]): TradeOrder =
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
