package com.flow.marketmaker.models

import java.util.UUID

import spray.json.{JsNumber, JsObject, JsString, JsValue}


abstract class SimpleConditional(val id: UUID) {
  def symbol: String

  def evaluate(test: Any): Boolean
}


object Indicator extends Enumeration {
  val Price = Value("price")
}


case class NullCondition(nid: UUID, val basePrice: Double = 0.0) extends SimpleConditional(nid) {
  lazy val symbol = "null"
  def evaluate(testPrice: Any) = false
}


case class GreaterThanEq(nid: UUID, basePrice: Double, description: String) extends SimpleConditional(nid) {
  lazy val symbol = ">="
  lazy val indicator = Indicator.Price
  lazy val conditionType = "simpleConditional"

  def evaluate(testPrice: Any) = testPrice.asInstanceOf[Double] >= basePrice
  override def toString = s"Price >= ${"%.8f".format(basePrice)}"
}


case class LessThanEq(nid: UUID, basePrice: Double, description: String) extends SimpleConditional(nid) {
  lazy val symbol = "<="
  lazy val indicator = Indicator.Price
  lazy val conditionType = "simpleConditional"

  def evaluate(testPrice: Any) = testPrice.asInstanceOf[Double] <= basePrice

  override def toString = s"Price <= ${"%.8f".format(basePrice)}"
}


object SimpleConditionalFactory {
  def createCondition(condId: UUID, operator: String, price: Double, description: String): SimpleConditional = {
    operator match {
      case "<=" => LessThanEq(condId, price, description)
      case ">=" => GreaterThanEq(condId, price, description)
      case _ => NullCondition(condId)
    }
  }
}

object JsonConditionTranslator {

  private def extract(key: String, fromCondition: JsObject): Option[JsValue] = {
    val fields: Map[String, JsValue] = fromCondition.fields
    fields.find ( _._1 == key ) match {
      case Some((key, value)) => Some(value)
      case _ => None
    }
  }

  private def convertToSimple(condition: JsObject, forOrderId: UUID): SimpleConditional = {
    val operator = extract("operator", condition).getOrElse(JsString("")).asInstanceOf[JsString].value
    val spotPrice = extract("value", condition).getOrElse(JsNumber(0)).asInstanceOf[JsNumber].value.toDouble
    val desc = extract("description", condition).getOrElse(JsString("")).asInstanceOf[JsString].value
    SimpleConditionalFactory.createCondition(forOrderId, operator, spotPrice, desc)
  }

  /**
    * A condition MUST have a conditionType
    * @param conditions
    * @param forOrderId is the id of the Order that this condition belongs to
    * @return list of SimpleConditionals
    */
  private def translate(conditions: Vector[JsValue], forOrderId: UUID): List[SimpleConditional] = {
    conditions.filter { cond =>
      // filter by conditionType where simpleConditional
      extract("conditionType", cond.asJsObject()) match {
        case Some(jsValue) => jsValue.asInstanceOf[JsString].value == "simpleConditional"
        case None => false
      }
    }.map { cond =>
      convertToSimple(cond.asJsObject(), forOrderId)
    }.toList

    // extend new conditions based on the conditionType here
  }

  //def fromOrder(order: Order): List[SimpleConditional] = {
  //  translate(order.conditionsToArray.elements, order.id)
  //}
}
