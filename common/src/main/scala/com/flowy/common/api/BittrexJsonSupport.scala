package com.flowy.common.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.flowy.common.models.Balance
import spray.json.DefaultJsonProtocol

object Bittrex {

  // Results
  case class DepositAddressResult(Currency: String, address: Option[String])

  case class OrderHistoryResult(OrderUuid: String,
                                Exchange: String,
                                TimeStamp: String,
                                OrderType: String,
                                Limit: Double,
                                Quantity: Double,
                                QuantityRemaining: Double,
                                Commission: Double,
                                Price: Double,
                                PricePerUnit: Double,
                                IsConditional: Boolean,
                                Condition: String,
                                ImmediateOrCancel: Boolean,
                                Closed: String)

  case class OrderResult(OrderUuid: String,
                         Exchange: String,
                         OrderType: String,
                         Limit: Double,
                         Quantity: Double,
                         QuantityRemaining: Double,
                         CommissionPaid: Double,
                         Price: Double,
                         CancelInitiated: Boolean,
                         Opened: String,
                         ImmediateOrCancel: Boolean,
                         IsConditional: Boolean)


  case class SingleOrderResult(OrderUuid: String,
                               Exchange: String,
                               Type: String,
                               Quantity: Double,
                               QuantityRemaining: Double,
                               Limit: Double,
                               Reserved: Double,
                               ReserveRemaining: Double,
                               CommissionReserved: Double,
                               CommissionReserveRemaining: Double,
                               CommissionPaid: Double,
                               Price: Double,
                               Opened: String,
                               IsOpen: Boolean,
                               Sentinel: String,
                               CancelInitiated: Boolean,
                               ImmediateOrCancel: Boolean,
                               IsConditional: Boolean)

  case class UUIDResult(uuid: String)

  case class WithdrawalResult(PaymentUuid: String,
                              Currency: String,
                              Amount: Double,
                              Address: String,
                              Opened: String,
                              Authorized: Boolean,
                              PendingPayment: Boolean,
                              TxCost: Double,
                              TxId: String,
                              Canceled: Boolean,
                              InvalidAddress: Boolean)

  case class DepositResult(Id: Long,
                           Amount: Double,
                           Currency: String,
                           Confirmations: Int,
                           LastUpdated: String,
                           TxId: String,
                           CryptoAddress: String)

  case class MarketResult(MarketCurrency: String,
                          BaseCurrency: String,
                          MarketCurrencyLong: String,
                          BaseCurrencyLong: String,
                          MinTradeSize: Double,
                          MarketName: String,
                          IsActive: Boolean,
                          Created: String)

  case class CurrencyResult(Currency: String,
                            CurrencyLong: String,
                            MinConfirmation: Int,
                            TxFee: Double,
                            IsActive: Boolean,
                            CoinType: String)

  case class TickerResult(Bid: Double,
                          Ask: Double,
                          Last: Double)

  case class MarketSummaryResult(MarketName: String,
                                 High: Double,
                                 Low: Double,
                                 Volume: Double,
                                 Last: Double,
                                 BaseVolume: Double,
                                 TimeStamp: String,
                                 Bid: Double,
                                 Ask: Double,
                                 OpenBuyOrders: Int,
                                 OpenSellOrders: Int,
                                 PrevDay: Double,
                                 Created: String)

  case class OrderBookEntryResult(Quantity: Double,
                                  Rate: Double)

  case class OrderBookResult(buy: List[OrderBookEntryResult],
                             sell: List[OrderBookEntryResult])

  case class MarketHistoryResult(Id: Long,
                                 TimeStamp: String,
                                 Quantity: Double,
                                 Price: Double,
                                 Total: Double,
                                 FillType: String,
                                 OrderType: String)

  // Standard bittrex response json has success, message, and option result
  case class StandardResponse[T](success: Boolean, message: String, result: Option[T])
  case class StandardNullResponse(success: Boolean, message: String)

  // All bittrex responses return some sort of result
  type BalanceResponse = StandardResponse[Balance]
  type BalancesResponse = StandardResponse[List[Balance]]
  type DepositAddressResponse = StandardResponse[DepositAddressResult]
  type OrderHistoryResponse = StandardResponse[List[OrderHistoryResult]]
  type GetOpenOrdersResponse = StandardResponse[List[OrderResult]]
  type UuidResponse = StandardResponse[UUIDResult]
  type SingleOrderResponse = StandardResponse[SingleOrderResult]
  type HistoryResponse = StandardResponse[List[WithdrawalResult]]
  type DepositResponse = StandardResponse[List[DepositResult]]
  type MarketResponse = StandardResponse[List[MarketResult]]
  type CurrencyResponse = StandardResponse[List[CurrencyResult]]
  type TickerResponse = StandardResponse[TickerResult]
  type MarketSummaryResponse = StandardResponse[List[MarketSummaryResult]]
  type OrderBookResponse = StandardResponse[OrderBookResult]
  type MarketHistoryResponse = StandardResponse[List[MarketHistoryResult]]
}

// collect your json format instances into a support trait:
trait BittrexJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  import Bittrex._

  // result formatters
  implicit val balanceResult          = jsonFormat5(Balance)
  implicit val depositAddressResult   = jsonFormat2(DepositAddressResult)
  implicit val orderHistoryResult     = jsonFormat14(OrderHistoryResult)
  implicit val openOrderResult        = jsonFormat12(OrderResult)
  implicit val orderUuidResult        = jsonFormat1(UUIDResult)
  implicit val singleOrderResult      = jsonFormat18(SingleOrderResult)
  implicit val histResult             = jsonFormat11(WithdrawalResult)
  implicit val depositResult          = jsonFormat7(DepositResult)
  implicit val marketResult           = jsonFormat8(MarketResult)
  implicit val currencyResult         = jsonFormat6(CurrencyResult)
  implicit val tickerResult           = jsonFormat3(TickerResult)
  implicit val marketSummaryResult    = jsonFormat13(MarketSummaryResult)
  implicit val orderBookEntryResult   = jsonFormat2(OrderBookEntryResult)
  implicit val orderBookResult        = jsonFormat2(OrderBookResult)
  implicit val marketHistoryResult    = jsonFormat7(MarketHistoryResult)

  // formatters for bittrex responses requires result formatters above
  implicit val balanceReponse         = jsonFormat3(StandardResponse[Balance])
  implicit val balancesResponse       = jsonFormat3(StandardResponse[List[Balance]])
  implicit val depositAddressResponse = jsonFormat3(StandardResponse[DepositAddressResult])
  implicit val orderHistResponse      = jsonFormat3(StandardResponse[List[OrderHistoryResult]])
  implicit val openOrderResponse      = jsonFormat3(StandardResponse[List[OrderResult]])
  implicit val orderUuidResponse      = jsonFormat3(StandardResponse[UUIDResult])
  implicit val nullResponse           = jsonFormat2(StandardNullResponse)
  implicit val singleOrderResponse    = jsonFormat3(StandardResponse[SingleOrderResult])
  implicit val histResponse           = jsonFormat3(StandardResponse[List[WithdrawalResult]])
  implicit val depositResponse        = jsonFormat3(StandardResponse[List[DepositResult]])
  implicit val marketResponse         = jsonFormat3(StandardResponse[List[MarketResult]])
  implicit val currencyResponse       = jsonFormat3(StandardResponse[List[CurrencyResult]])
  implicit val tickerResponse         = jsonFormat3(StandardResponse[TickerResult])
  implicit val marketSummaryResponse  = jsonFormat3(StandardResponse[List[MarketSummaryResult]])
  implicit val orderBookResponse      = jsonFormat3(StandardResponse[OrderBookResult])
  implicit val marketHistoryResponse  = jsonFormat3(StandardResponse[List[MarketHistoryResult]])
}


