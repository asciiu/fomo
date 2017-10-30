package com.flow.marketmaker.models

import java.time.{Instant, ZoneOffset}
import java.util.UUID
import org.scalatest.{FlatSpec, Matchers}
import spray.json._


class OrderSpec extends FlatSpec with Matchers {

  def validOrder(): Order = {
    val jsonCond1 = JsObject(
      "conditionType" -> JsString("simpleConditional"),
      "indicator" -> JsString("price"),
      "operator" -> JsString("<="),
      "value" -> JsNumber(.007),
      "description" -> JsString("price <= 0.007"))

    val jsonCond2 = JsObject(
      "conditionType" -> JsString("simpleConditional"),
      "indicator" -> JsString("price"),
      "operator" -> JsString(">="),
      "value" -> JsNumber(.0065),
      "description" -> JsString("price >= 0.0065"))

    val jsonConditions: JsValue = JsArray(jsonCond1, jsonCond2).asInstanceOf[JsValue]
    val userId = UUID.randomUUID()
    val orderId = 5

    Order(
      id = Some(orderId),
      userId = userId,
      exchangeName = "random",
      marketName = "BTC-HOT",
      createdTime = Instant.now().atOffset(ZoneOffset.UTC),
      orderType = OrderType.Buy,
      quantity = 1000,
      status = OrderStatus.Pending,
      conditions = jsonConditions)
  }

  "Order" should "accept JsValue as conditions with valid conditions" in {
    val order = validOrder()
    val orderId = order.id
    val conds = order.conditionsToArray
    conds.elements.length should be(2)

    val simpleConditions = JsonConditionTranslator.fromOrder(order)
    simpleConditions.length should be(2)
    Some(simpleConditions(0).orderId) should be(orderId)
    simpleConditions(0).evaluate(.0005) should be(true)
  }

  "Order" should "accept JsValue as conditions with valid conditions" in {
    val order = validOrder()
    val orderId = order.id
    val conds = order.conditionsToArray
    conds.elements.length should be(2)

    val simpleConditions = JsonConditionTranslator.fromOrder(order)
    val passConditions = simpleConditions.filter(_.evaluate(0.00010))

    passConditions.length should be(1)
    Some(passConditions(0).orderId) should be(orderId)
  }

//  "Order" should "evaulate to true" in {
//    val condition = LessThanEq(0.00069999)
//    val userId = UUID.randomUUID()
//
//    val order = new Order(
//      id = None,
//      userId = userId,
//      exchangeName = "random",
//      marketName = "BTC-HOT",
//      createdTime = Instant.now().atOffset(ZoneOffset.UTC),
//      orderType = OrderType.Buy,
//      quantity = 1000,
//      status = OrderStatus.Pending,
//      orConditions = List(condition))
//
//    order.isCondition(0.00069999) should be(true)
//  }
//
//  "Order" should "evaulate to false" in {
//    val condition = LessThanEq(0.00069999)
//    val userId = UUID.randomUUID()
//
//    val order = new Order(
//      id = None,
//      userId = userId,
//      exchangeName = "random",
//      marketName = "BTC-HOT",
//      createdTime = Instant.now().atOffset(ZoneOffset.UTC),
//      orderType = OrderType.Buy,
//      quantity = 1000,
//      status = OrderStatus.Pending,
//      orConditions = List(condition))
//
//    order.isCondition(0.00070000) should be(false)
//  }
}
