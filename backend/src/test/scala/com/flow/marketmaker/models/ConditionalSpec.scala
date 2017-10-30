package com.flow.marketmaker.models

import org.scalatest.{FlatSpec, Matchers}

class ConditionalSpec extends FlatSpec with Matchers  {

  "GreaterThanEq" should "evaluate to false" in {
    // given
    val price = 0.0003000

    val condition = GreaterThanEq(1, 0.00043490, "Price >= 0.00043490")
    val result = condition.evaluate(price)

    result should be(false)
  }

  "GreaterThanEq" should "evaluate to true" in {
    // given
    val price = 0.0003000

    val condition = GreaterThanEq(2, 0.00001000, "Price >= 0.0001")
    val result = condition.evaluate(price)

    result should be(true)
  }

  "LessThanEq" should "evaluate to true" in {
    // given
    val price = 0.0007000

    val condition = LessThanEq(3, 0.00070001, "Price <= 0.00070001")
    val result = condition.evaluate(price)

    result should be(true)
  }

  "LessThanEq" should "evaluate to false" in {
    // given
    val price = 0.0007000

    val condition = LessThanEq(4, 0.00069999, "Price <= 00069999")
    val result = condition.evaluate(price)

    result should be(false)
  }
}
