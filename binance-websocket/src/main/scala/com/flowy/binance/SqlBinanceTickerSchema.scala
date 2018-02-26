package com.flowy.binance

import java.time.OffsetDateTime

import com.flowy.common.models.{MarketInfo, Trade, TradeStat}
import com.flowy.common.models.MarketStructures.MarketUpdate
import com.flowy.common.utils.sql.SqlDatabase


/**
  * The schemas are in separate traits, so that if your DAO would require to access (e.g. join) multiple tables,
  * you can just mix in the necessary traits and have the `TableQuery` definitions available.
  */
trait SqlBinanceTickerSchema {

  protected val database: SqlDatabase

  import database._
  import database.driver.api._

  protected val binanceTickers = TableQuery[BinanceTickers]

  protected class BinanceTickers(tag: Tag) extends Table[BinanceTicker](tag, "binance_24hr_tickers") {
    def id                   = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def eventType            = column[String]("event_type")
    def eventTime            = column[Long]("event_time")
    def symbol               = column[String]("symbol")
    def priceChange          = column[String]("price_change")
    def priceChangePercent   = column[String]("price_change_percent")
    def weightedAveragePrice = column[String]("weighted_average_price")
    def previousDayClose     = column[String]("previous_day_close")
    def currentDayClose      = column[String]("current_day_close")
    def closeTradeQty        = column[String]("close_trade_qty")
    def bestBidPrice         = column[String]("best_bid_price")
    def bestBidQty           = column[String]("best_bid_qty")
    def bestAskPrice         = column[String]("best_ask_price")
    def bestAskQty           = column[String]("best_ask_qty")
    def open                 = column[String]("open")
    def high                 = column[String]("high")
    def low                  = column[String]("low")
    def baseVolume           = column[String]("base_volume")
    def volume               = column[String]("volume")
    def openTime             = column[Long]("open_time")
    def closeTime            = column[Long]("close_time")
    def firstTradeId         = column[Long]("first_trade_id")
    def lastTradeId          = column[Long]("last_trade_id")
    def totalTrades          = column[Long]("total_trades")

    def frag1 = (eventType,
                 eventTime,
                 symbol) <> ((BinanceTickerFrag1.apply _).tupled, BinanceTickerFrag1.unapply)

    def frag2 = (priceChange,
                 priceChangePercent,
                 weightedAveragePrice,
                 previousDayClose,
                 currentDayClose,
                 closeTradeQty,
                 bestBidPrice,
                 bestBidQty,
                 bestAskPrice,
                 bestAskQty,
                 open,
                 high,
                 low,
                 baseVolume,
                 volume,
                 openTime,
                 closeTime,
                 firstTradeId,
                 lastTradeId,
                 totalTrades) <> ((BinanceTickerFrag2.apply _).tupled, BinanceTickerFrag2.unapply)

    def * = (frag1, frag2) <> ((BinanceTicker.apply _).tupled, BinanceTicker.unapply)
  }
}

