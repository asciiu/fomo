package com.flowy.bittrexExchange.messages

import java.util.UUID

case class GetStopLosses(userId: UUID, marketName: String)

case class TrailingStop(userId: UUID, marketName: String, percent: Double, refPrice: Double) {

  private var referencePrice: Double = refPrice

  def update(price: Double): Boolean = {
    if (referencePrice < price) {
      referencePrice = price
      true
    } else {
      false
    }
  }

  def triggerPrice(): Double = referencePrice * (1.0 - percent)

  def isStop(price: Double): Boolean = price <= triggerPrice
}
