package com.flow.marketmaker.models

import java.util.UUID

import com.flow.marketmaker.models.TradeType.Value



object TradeType extends Enumeration {
  val Buy = Value("buy")
  val Sell    = Value("sell")
}

class TradeOrder(val userId: UUID,
                 val exchangeName: String,
                 val marketName: String,
                 val currencyName: String,
                 val side: TradeType.Value,
                 val quantity: Double,
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
