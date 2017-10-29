package com.flow.marketmaker.models


abstract class Conditional() {
  def symbol: String

  def evaluate(test: Any): Boolean
  def toJson(): String
}


object Indicator extends Enumeration {
  val Price = Value("price")
}


case class NullCondition(val basePrice: Double = 0.0) extends Conditional  {
  lazy val symbol = "null"
  def evaluate(testPrice: Any) = false
  def toJson() = "null"
}


case class GreaterThanEq(basePrice: Double) extends Conditional {
  lazy val symbol = ">="
  lazy val indicator = Indicator.Price
  lazy val conditionType = "simpleConditional"

  def evaluate(testPrice: Any) = testPrice.asInstanceOf[Double] >= basePrice
  override def toString = s">= ${"%.8f".format(basePrice)}"

  def toJson() =
    s"""{"conditionType": "$conditionType", "indicator": "$indicator",
       | "operator": "$symbol", "value": ${"%.8f".format(basePrice)},
       | "description":"$toString"}""".stripMargin
}


case class LessThanEq(basePrice: Double) extends Conditional {
  lazy val symbol = "<="
  lazy val indicator = Indicator.Price
  lazy val conditionType = "simpleConditional"

  def evaluate(testPrice: Any) = testPrice.asInstanceOf[Double] <= basePrice

  override def toString = s"<= ${"%.8f".format(basePrice)}"
  def toJson() =
    s"""{"conditionType": "$conditionType", "indicator": "$indicator",
       | "operator": "$symbol", "value": ${"%.8f".format(basePrice)},
       | "description":"$toString"}""".stripMargin
}


object SimpleConditionalFactory {
  def makeCondition(str: String, value: Double) = {
    str match {
      case "<=" => LessThanEq(value)
      case ">=" => GreaterThanEq(value)
    }
  }
}
