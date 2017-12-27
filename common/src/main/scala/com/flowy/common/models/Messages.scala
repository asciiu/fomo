package com.flowy.common.models

import java.time.OffsetDateTime
import java.util.UUID


case object TrailingStopLossRegistration
case object BittrexWebsocketClientRegistration

case class PriceCheck(exchangeName: Exchange.Value, marketName: String, price: BigDecimal)

case class TradeHistory(id: UUID,
                        userId: UUID,
                        tradeId: UUID,
                        exchangeName: String,
                        marketName: String,
                        currency: String,
                        currencyLong: String,
                        currencyQty: BigDecimal,
                        baseCurrency: String,
                        baseCurrencyLong: String,
                        baseQty: BigDecimal,
                        tradeAction: TradeAction.Value,
                        bidAskPrice: BigDecimal,
                        actualPrice: BigDecimal,
                        title: String,
                        summary: String,
                        createdOn: OffsetDateTime,
                        updatedOn: OffsetDateTime
                       )

object TradeHistory {
  def createInstance(userId: UUID,
                 tradeId: UUID,
                 exchangeName: String,
                 marketName: String,
                 currency: String,
                 currencyLong: String,
                 currencyQty: BigDecimal,
                 baseCurrency: String,
                 baseCurrencyLong: String,
                 baseQty: BigDecimal,
                 tradeAction: TradeAction.Value,
                 bidAskPrice: BigDecimal,
                 actualPrice: BigDecimal,
                 title: String,
                 summary: String): TradeHistory = {

    TradeHistory(
      UUID.randomUUID(),
      userId,
      tradeId,
      exchangeName,
      marketName,
      currency,
      currencyLong,
      currencyQty,
      baseCurrency,
      baseCurrencyLong,
      baseQty,
      tradeAction,
      bidAskPrice,
      actualPrice,
      title,
      summary,
      OffsetDateTime.now(),
      OffsetDateTime.now())
  }
}
