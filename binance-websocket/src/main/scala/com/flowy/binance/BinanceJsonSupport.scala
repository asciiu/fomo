package com.flowy.binance

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.flowy.common.models.MarketStructures.MarketUpdate
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat}
import spray.json._

import scala.util.Try


// https://github.com/binance-exchange/binance-official-api-docs/blob/master/web-socket-streams.md#all-market-tickers-stream
case class Binance24HrTicker(
  e: String,   // "24hrTicker",   Event type
  E: Long,     // 123456789,      Event time
  s: String,   // "BNBBTC",       Symbol
  p: String,   // "0.0015",       Price change
  P: String,   // "250.00",       Price change percent
  w: String,   // "0.0018",       Weighted average price
  x: String,   // "0.0009",       Previous day's close price
  c: String,   // "0.0025",       Current day's close price
  Q: String,   // "10",           Close trade's quantity
  b: String,   // "0.0024",       Best bid price
  B: String,   // "10",           Bid bid quantity
  a: String,   // "0.0026",       Best ask price
  A: String,   // "100",          Best ask quantity
  o: String,   // "0.0010",       Open price
  h: String,   // "0.0025",       High price
  l: String,   // "0.0010",       Low price
  v: String,   // "10000",        Total traded base asset volume
  q: String,   // "18",           Total traded quote asset volume
  O: Long,     // 0,              Statistics open time
  C: Long,     // 86400000,       Statistics close time
  F: Long,     // 0,              First trade ID
  L: Long,     // 18150,          Last trade Id
  n: Long      // 18151           Total number of trades
 )

case class BinanceTicker(frag1: BinanceTickerFrag1, frag2: BinanceTickerFrag2)

case class BinanceTickerFrag1(eventType: String,
                              eventTime: Long,
                              symbol: String)

case class BinanceTickerFrag2(priceChange: String,
                              priceChangePercent: String,
                              weightedAveragePrice: String,
                              previousDayClose: String,
                              currentDayClose: String,
                              closeTradeQty: String,
                              bestBidPrice: String,
                              bestBidQty: String,
                              bestAskPrice: String,
                              bestAskQty: String,
                              open: String,
                              high: String,
                              low: String,
                              baseVolume: String,
                              volume: String,
                              openTime: Long,
                              closeTime: Long,
                              firstTradeId: Long,
                              lastTradeId: Long,
                              totalTrades: Long)
