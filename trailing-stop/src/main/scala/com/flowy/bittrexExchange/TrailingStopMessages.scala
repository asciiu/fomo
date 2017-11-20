package com.flowy.trailingStop.messages

import java.util.UUID

case class GetStopLosses(userId: UUID, marketName: String)

case class TrailingStop(userId: UUID, tradeId: UUID, marketName: String, percent: Double, refPrice: Double) {

  private var ceilingPrice: Double = refPrice

  def update(price: Double): Boolean = {
    if (ceilingPrice < price) {
      println(s"new ceiling ${price}")
      ceilingPrice = price
      true
    } else {
      println(s"current trigger ${triggerPrice}")
      false
    }
  }

  def triggerPrice: Double = ceilingPrice * (1.0 - percent)

  def isStop(price: Double): Boolean = price <= triggerPrice

  override def toString() = s"userId: $userId tradeId: $tradeId marketName: $marketName triggerPrice:$triggerPrice"
}
