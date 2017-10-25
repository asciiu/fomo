package com.flow.marketmaker.models

import java.util.UUID

import org.scalatest.{FlatSpec, Matchers}

class TradeOrderSpec extends FlatSpec with Matchers {

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

  "TradeOrder" should "evaulate to true" in {
    val condition = LessThanEq(0.00069999)
    val userId = UUID.randomUUID()

    val trade = new TradeOrder(
      userId = userId,
      exchangeName = "random",
      marketName = "BTC-HOT",
      currencyName = "HOT",
      side = TradeType.Buy,
      quantity = 1000,
      orConditions = List(condition))

    trade.evaluate(0.00069999) should be(true)
  }

  "TradeOrder" should "evaulate to false" in {
    val condition = LessThanEq(0.00069999)
    val userId = UUID.randomUUID()

    val trade = new TradeOrder(
      userId = userId,
      exchangeName = "random",
      marketName = "BTC-HOT",
      currencyName = "HOT",
      side = TradeType.Buy,
      quantity = 1000,
      orConditions = List(condition))

    trade.evaluate(0.00070000) should be(false)
  }
}
