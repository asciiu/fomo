package com.flowy.bittrexExchange

import java.util.UUID
import messages.TrailingStop

class StopLossCollection(marketName: String, lprice: Double) {
  private val collection = new scala.collection.mutable.ListBuffer[TrailingStop]()

  private var lastPrice: Double = lprice

  def last = lastPrice

  def addStopLoss(stop: TrailingStop) = {
    collection += stop
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
