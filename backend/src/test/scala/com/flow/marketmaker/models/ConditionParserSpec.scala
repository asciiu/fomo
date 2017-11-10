package com.flow.marketmaker.models

import org.scalatest.{FlatSpec, Matchers}
import scala.reflect.runtime.universe
import scala.tools.reflect.ToolBox

class ConditionParserSpec extends FlatSpec with Matchers {
  val conditionA = "(price >= 0.00005000) || trailingStopLoss(0.05)"
  val conditionB = "(price <= 0.00045)"

  "ConditionParser" should "parse price conditions" in {
    val condition = "(price >= 0.00045)"
    val price = 0.5
    val condition = s"($price >= 0.4 && $price <= 0.6)"
    val tb = universe.runtimeMirror(getClass.getClassLoader).mkToolBox()
    tb.eval(tb.parse(s"println($condition)"))

    true should be(true)
  }
}
