package com.flowy.marketmaker.models

import MarketStructures.{ClosePrice, ExponentialMovingAverage}
import com.flowy.marketmaker.Util
import scala.collection.mutable.ListBuffer
import scala.math.BigDecimal.RoundingMode

/**
  * Keeps track of the exponential moving averages
  * for each time period.
  *
  * @param marketName
  * @param period how many periods for example 10 period, 5 period, etc
  * @param periodMinutes length of period in minutes. Example 5
  * @param historicClosePrices assumes the close prices are in decending order
  *                            and that all close prices have the same periodMinutes
  *                            as this class. Example, close prices for every 5 minute
  *                            period for the last 24 hour window.
  */
class MarketEMACollection(val marketName: String,
                          val period: Int,
                          val periodMinutes: Int,
                          historicClosePrices: List[ClosePrice]) {
  require(period > 0)

  // enough close prices needed to start the
  // initial computation
  require(historicClosePrices.length >= period)

  private val movingAverages = ListBuffer[ExponentialMovingAverage]()

  def emas = movingAverages.toList

  setAverages(historicClosePrices)

  /**
    * Multiplier used for weighting exponential moving average.
    *
    * @param periodNum
    * @return
    */
  private def multiplier(periodNum: Int) : BigDecimal = {
    BigDecimal(2.0 / (periodNum + 1))
  }

  /**
    * EMA: {Close - EMA(previous day)} x multiplier + EMA(previous day)
    */
  private def ema(close: BigDecimal, previousEMA: BigDecimal): BigDecimal = {
    (close - previousEMA) * multiplier(period) + previousEMA
  }

  /**
    * Sets the moving averages for market name based upon the received closing prices.
    * The moving average time is dictated by the period field.
    *
    *  formula = {Close - EMA(previous day)} x multiplier + EMA(previous day)
    *  source: http://stockcharts.com/school/doku.php?id=chart_school:technical_indicators:moving_averages
    *
    * @param closePrices - closing prices list MUST be ordered by descending date - i.e. most recent
    *                    dated close price should be at head.
    */
  private def setAverages(closePrices: List[ClosePrice]) = {
    // sum of the last close prices within the period
    val sum = closePrices.takeRight(period).foldLeft(BigDecimal(0))( (a, p) => p.price + a)
    val simpleMovingAverage = sum / period

    val firstClose = closePrices(closePrices.length - period)

    // exponential moving average begins with a simple moving average
    ExponentialMovingAverage(firstClose.time, simpleMovingAverage, firstClose.price) +=: movingAverages

    // loop through all closing prices from firstClose and compute exponential moving average
    for (i <- period+1 to closePrices.length) {
      val previousEMA = movingAverages.head.ema

      val closePrice = closePrices(closePrices.length - i)

      //val ema = (closePrice.price - previousEMA) * multiplier(period) + previousEMA
      val a = ema(closePrice.price, previousEMA)
      // most recent average always at head
      ExponentialMovingAverage(closePrice.time, a.setScale(8, RoundingMode.CEILING), closePrice.price) +=: movingAverages

    }
  }

  /**
    * Updates the latest moving average. Assumes that the close price
    * received is the latest close price.
    *
    * @param closePrice - the latest close price. ClosePrice timestamp must
    *                   be after the timestamp of the latest average in moving
    *                   averages.
    * @return
    */
  def updateAverages(closePrice: ClosePrice): ExponentialMovingAverage = {
    val normalizedTime = Util.roundDateToMinute(closePrice.time, periodMinutes)
    val latestAvg = movingAverages.head

    val average =
      if (normalizedTime.isBefore(latestAvg.time)) {
        latestAvg
      } else if (latestAvg.time.isEqual(normalizedTime)) {
        val previousEMA = movingAverages(1).ema
        val a = ema(closePrice.price, previousEMA)
        val expo = ExponentialMovingAverage(normalizedTime, a.setScale(8, RoundingMode.CEILING), closePrice.price)

        // update the latest moving average at head
        movingAverages.update(0, expo)
        expo
      } else {

        // fill in skipped periods
        if (latestAvg.time.plusMinutes(periodMinutes).isBefore(normalizedTime)) {
          val deltaSeconds = normalizedTime.toEpochSecond - latestAvg.time.toEpochSecond
          val skippedPeriods = (deltaSeconds / (periodMinutes * 60L)).toInt - 1
          for (i <- 1 to skippedPeriods) {
            // add appropriate 5 minute interval to compute next candle time
            val previousEMA = movingAverages.head.ema
            val lastClosePrice = movingAverages.head.atPrice
            val time = latestAvg.time.plusMinutes(periodMinutes * i)
            val a = ema(lastClosePrice, previousEMA)
            ExponentialMovingAverage(time, a.setScale(8, RoundingMode.CEILING), lastClosePrice) +=: movingAverages
          }
        }

         val previousEMA = movingAverages.head.ema
         val a = ema(closePrice.price, previousEMA)
         val expo = ExponentialMovingAverage(normalizedTime, a.setScale(8, RoundingMode.CEILING), closePrice.price)

         // insert new average at head
         expo +=: movingAverages
         expo
      }

    // limit movingAverages to 24 hours
    if (movingAverages.length > 288) {
      val removeNum = movingAverages.length - 288
      movingAverages.remove(288, removeNum)
    }
    average
  }
}
