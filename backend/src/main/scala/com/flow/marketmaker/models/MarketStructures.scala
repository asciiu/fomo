package com.flow.marketmaker.models

import java.time.OffsetDateTime

/**
  * Created by bishop on 10/18/16.
  */
object MarketStructures {
  case class ExponentialMovingAverage(time: OffsetDateTime, ema: BigDecimal, atPrice: BigDecimal)
  case class Candles(marketName: String, candles: List[MarketCandle])
  case class ClosePrice(time: OffsetDateTime, price: BigDecimal)

  case class CandleClosePrices(marketName: String, closePrices: List[ClosePrice])
  case class EMA(time: OffsetDateTime, ema: BigDecimal)
  case class PeriodVolume(time: OffsetDateTime, btcVolume: BigDecimal)
  case class BollingerBandPoint(time: OffsetDateTime, center: BigDecimal, upper: BigDecimal, lower: BigDecimal)

  // TODO we need a new generic case class here that
  // original struct used for polo
  case class MarketMessage(time: OffsetDateTime,
                           cryptoCurrency: String,
                           last: BigDecimal,
                           lowestAsk: BigDecimal,
                           highestBid: BigDecimal,
                           percentChange: BigDecimal,
                           baseVolume: BigDecimal,
                           quoteVolume: BigDecimal,
                           isFrozen: String,
                           high24hr: BigDecimal,
                           low24hr: BigDecimal)

  // new struct used for bittrex
  case class MarketUpdate(MarketName: String,
                         High: Float,
                         Low: Float,
                         Volume: Float,
                         Last: Float,
                         BaseVolume: Float,
                         TimeStamp: String,
                         Bid: Float,
                         Ask: Float,
                         OpenBuyOrders: Int,
                         OpenSellOrders: Int,
                         PrevDay: Float,
                         Created: String)


   case class PriceUpdateBTC(time: OffsetDateTime, last: BigDecimal)

  /**
    * Represents an Order.
    * @param time
    * @param marketName
    * @param price
    * @param quantity
    * @param side
    * @param callback a callback function that is invoked when the order is filled.
    *                 example: def incrementSellFill(order: Order, fillTime: OffsetDateTime)
    */
  case class Order(time: OffsetDateTime,
                   marketName: String,
                   price: BigDecimal,
                   quantity: BigDecimal,
                   side: String,
                   callback: (Order, OffsetDateTime) => Unit)

  case class OrderBookModify(marketName: String, side: String, rate: BigDecimal, amount: BigDecimal)

  case class OrderBookRemove(marketName: String, side: String, rate: BigDecimal)

  case class Trade(marketName: String,
                   time: OffsetDateTime,
                   tradeID: Int,
                   side: String,
                   rate : BigDecimal,
                   amount: BigDecimal,
                   total: BigDecimal
                  )

  //case class Trade(marketName: String,
  //                 time: OffsetDateTime,
  //                 price: BigDecimal,
  //                 quantity: BigDecimal)

  case class MarketSetupNotification(marketName: String, isSetup: Boolean)
}
