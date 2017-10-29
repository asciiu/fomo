package com.flow.marketmaker.models

import java.time.{Instant, ZoneOffset}
import java.util.UUID

import org.scalatest.{FlatSpec, Matchers}

class OrderSpec extends FlatSpec with Matchers {

  "GreaterThanEq" should "evaluate to false" in {
    // given
    val price = 0.0003000

    val condition = GreaterThanEq(0.00043490)
    val result = condition.evaluate(price)

    result should be(false)
  }

  "GreaterThanEq" should "evaluate to true" in {
    // given
    val price = 0.0003000

    val condition = GreaterThanEq(0.00001000)
    val result = condition.evaluate(price)

    result should be(true)
  }

  "LessThanEq" should "evaluate to true" in {
    // given
    val price = 0.0007000

    val condition = LessThanEq(0.00070001)
    val result = condition.evaluate(price)

    result should be(true)
  }

  "LessThanEq" should "evaluate to false" in {
    // given
    val price = 0.0007000

    val condition = LessThanEq(0.00069999)
    val result = condition.evaluate(price)

    result should be(false)
  }

  "Order" should "evaulate to true" in {
    val condition = LessThanEq(0.00069999)
    val userId = UUID.randomUUID()

    val order = new Order(
      id = None,
      userId = userId,
      exchangeName = "random",
      marketName = "BTC-HOT",
      createdTime = Instant.now().atOffset(ZoneOffset.UTC),
      orderType = OrderType.Buy,
      quantity = 1000,
      status = OrderStatus.Pending,
      orConditions = List(condition))

    order.isCondition(0.00069999) should be(true)
  }

  "Order" should "evaulate to false" in {
    val condition = LessThanEq(0.00069999)
    val userId = UUID.randomUUID()

    val order = new Order(
      id = None,
      userId = userId,
      exchangeName = "random",
      marketName = "BTC-HOT",
      createdTime = Instant.now().atOffset(ZoneOffset.UTC),
      orderType = OrderType.Buy,
      quantity = 1000,
      status = OrderStatus.Pending,
      orConditions = List(condition))

    order.isCondition(0.00070000) should be(false)
  }
}
