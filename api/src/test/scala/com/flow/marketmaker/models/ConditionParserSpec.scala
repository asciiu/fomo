package com.flow.marketmaker.models

import org.scalatest.{FlatSpec, Matchers}
import scala.tools.reflect.ToolBox
import scala.reflect.runtime.currentMirror


class ConditionParserSpec extends FlatSpec with Matchers {
  val conditionA = "(price >= 0.00005000) || trailingStopLoss(0.05)"
  val conditionB = "(price <= 0.00045)"

  "Evaluating a condition" should "return a boolean" in {
    val condition = "(price >= 0.00045)"
    val price = 0.5

    val tb = currentMirror.mkToolBox()
    val result = tb.eval(tb.parse(condition.replace("price", price.toString))) == true

    result should be(true)
  }
}
