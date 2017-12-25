package com.flowy.common

import org.scalatest.FlatSpec

import scala.collection.mutable

class UtilSpec extends FlatSpec {

  "Round precision" should "round within 4" in {
    val result = Util.roundUpPrecision4(1.00009999)

    assert(result === 1.00010000)
  }
}
