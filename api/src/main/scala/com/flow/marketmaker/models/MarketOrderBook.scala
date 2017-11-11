package com.flow.marketmaker.models

case class OrderLine(rate: BigDecimal, amount: BigDecimal)
case class OrderBook(asks: List[OrderLine], bids: List[OrderLine])

class MarketOrderBook(val marketName: String) {

  private val bids = scala.collection.mutable.Map[BigDecimal, BigDecimal]()

  private val asks = scala.collection.mutable.Map[BigDecimal, BigDecimal]()

  def allAsks: List[OrderLine] = {
    asks.toList.map ( o => OrderLine(o._1, o._2))
  }

  def allBids: List[OrderLine] = {
    bids.toList.map ( o => OrderLine(o._1, o._2))
  }

  def bids(b: List[OrderLine]): Map[BigDecimal, BigDecimal] = {
    val e = b.map(o => o.rate -> o.amount).toMap
    bids ++= e
    bids.toMap
  }

  def asks(a: List[OrderLine]): Map[BigDecimal, BigDecimal] = {
    val e = a.map(o => o.rate -> o.amount).toMap
    asks ++= e
    asks.toMap
  }

  def removeAsks(atRate: BigDecimal) = {
    asks.remove(atRate)
  }

  def updateAsks(atRate: BigDecimal, amount: BigDecimal) = {
    asks.update(atRate, amount)
  }

  def removeBids(atRate: BigDecimal) = {
    bids.remove(atRate)
  }

  def updateBids(atRate: BigDecimal, amount: BigDecimal) = {
    bids.update(atRate, amount)
  }


}
