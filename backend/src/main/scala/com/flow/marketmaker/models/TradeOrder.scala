package com.flow.marketmaker.models

import java.util.UUID

import com.flow.marketmaker.models.TradeType.Value



object TradeType extends Enumeration {
  val Buy = Value("buy")
  val Sell    = Value("sell")
}

class TradeOrder(userId: UUID,
                 exchangeName: String,
                 marketName: String,
                 currencyName: String,
                 side: TradeType.Value,
                 quantity: Double,
                 orConditions: List[TradeCondition]
                ) {

  // evaluates all conditions and returns true if any of the conditions are true
  def evaluate(test: Any): Boolean = orConditions.exists( _.evaluate(test) )
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
}

case class LessThanEq(basePrice: Double) extends TradeCondition {
  lazy val symbol = "<="
  lazy val indicator = Indicator.Price

  def evaluate(testPrice: Any) = testPrice.asInstanceOf[Double] <= basePrice
}
