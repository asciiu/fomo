package com.flowy.trailstop

import java.util.UUID

class StopLossCollection(marketName: String, lprice: Double) {
  private val collection = new scala.collection.mutable.ListBuffer[TrailingStop]()

  private var lastPrice: Double = lprice

  def last = lastPrice

  def addStopLoss(stop: TrailingStop) = {
    if (stop.refPrice <= 0.0) {
      collection += stop.copy(refPrice = lastPrice)
    } else {
      collection += stop
    }
  }

  def getStopLosses(userId: UUID): Seq[TrailingStop] = {
    collection.filter( _.userId == userId )
  }

  def updateStopLosses(price: Double) = {
    lastPrice = price
    collection.foreach( _.update(price) )
  }

  def triggeredStopLossesRemoved(price: Double): Seq[TrailingStop] = {
    val stops = collection.filter( _.isStop(price) )
    collection --= stops
    stops
  }
}
