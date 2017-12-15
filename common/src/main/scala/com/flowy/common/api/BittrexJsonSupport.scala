package com.flowy.common.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.flowy.common.models.Balance
import spray.json.DefaultJsonProtocol

object Bittrex {

  // Results
  case class DepositAddressResult(currency: String, address: Option[String])

  case class OrderHistoryResult(orderUuid: String,
                                exchange: String,
                                timeStamp: String,
                                orderType: String,
                                limit: Double,
                                quantity: Double,
                                quantityRemaining: Double,
                                commission: Double,
                                price: Double,
                                pricePerUnit: Double,
                                isConditional: Boolean,
                                condition: String,
                                immediateOrCancel: Boolean,
                                closed: String)

  case class OrderResult(orderUuid: String,
                         exchange: String,
                         orderType: String,
                         limit: Double,
                         quantity: Double,
                         quantityRemaining: Double,
                         commissionPaid: Double,
                         price: Double,
                         cancelInitiated: Boolean,
                         opened: String,
                         immediateOrCancel: Boolean,
                         isConditional: Boolean)


  case class SingleOrderResult(orderUuid: String,
                               exchange: String,
                               orderType: String,
                               quantity: Double,
                               quantityRemaining: Double,
                               limit: Double,
                               reserved: Double,
                               reserveRemaining: Double,
                               commissionReserved: Double,
                               commissionReserveRemaining: Double,
                               commissionPaid: Double,
                               price: Double,
                               opened: String,
                               isOpen: Boolean,
                               sentinel: String,
                               cancelInitiated: Boolean,
                               immediateOrCancel: Boolean,
                               isConditional: Boolean)

  case class UUIDResult(uuid: String)

  case class WithdrawalResult(paymentUuid: String,
                              currency: String,
                              amount: Double,
                              address: String,
                              opened: String,
                              authorized: Boolean,
                              pendingPayment: Boolean,
                              txCost: Double,
                              txId: String,
                              canceled: Boolean,
                              invalidAddress: Boolean)

  case class DepositResult(id: Long,
                           amount: Double,
                           currency: String,
                           confirmations: Int,
                           lastUpdated: String,
                           txId: String,
                           cryptoAddress: String)

  case class MarketResult(marketCurrency: String,
                          baseCurrency: String,
                          marketCurrencyLong: String,
                          baseCurrencyLong: String,
                          minTradeSize: Double,
                          marketName: String,
                          isActive: Boolean,
                          created: String)

  case class CurrencyResult(currency: String,
                            currencyLong: String,
                            minConfirmation: Int,
                            txFee: Double,
                            isActive: Boolean,
                            coinType: String)

  case class TickerResult(bid: Double,
                          ask: Double,
                          last: Double)

  case class MarketSummaryResult(marketName: String,
                                 high: Double,
                                 low: Double,
                                 volume: Double,
                                 last: Double,
                                 baseVolume: Double,
                                 timeStamp: String,
                                 bid: Double,
                                 ask: Double,
                                 openBuyOrders: Int,
                                 openSellOrders: Int,
                                 prevDay: Double,
                                 created: String)

  case class OrderBookEntryResult(quantity: Double,
                                  rate: Double)

  case class OrderBookResult(buy: List[OrderBookEntryResult],
                             sell: List[OrderBookEntryResult])

  case class MarketHistoryResult(id: Long,
                                 timeStamp: String,
                                 quantity: Double,
                                 price: Double,
                                 total: Double,
                                 fillType: String,
                                 orderType: String)

  // Standard bittrex response json has success, message, and option result
  case class StandardResponse[T](success: Boolean, message: String, result: Option[T])
  case class StandardNullResponse(success: Boolean, message: String)
  case class BalancesAuthorization(auth: Auth, response: BalancesResponse)

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
  implicit val balanceResult          = jsonFormat(Balance, "Currency", "Balance", "Available", "Pending", "CryptoAddress")
  implicit val depositAddressResult   = jsonFormat(DepositAddressResult, "Currency", "Address")
  implicit val orderHistoryResult     = jsonFormat(OrderHistoryResult, "OrderUuid", "Exchange", "TimeStamp", "OrderType", "Limit", "Quantity", "QuantityRemaining", "Commission", "Price" , "PricePerUnit", "IsConditional", "Condition", "ImmediateOrCancel" ,"Closed")
  implicit val openOrderResult        = jsonFormat(OrderResult, "OrderUuid", "Exchange", "OrderType", "Limit", "Quantity", "QuantityRemaining", "CommissionPaid", "Price", "CancelInitiated", "Opened", "ImmediateOrCancel", "IsConditional")
  implicit val orderUuidResult        = jsonFormat1(UUIDResult)
  implicit val singleOrderResult      = jsonFormat(SingleOrderResult, "OrderUuid", "Exchange", "Type", "Quantity", "QuantityRemaining", "Limit", "Reserved", "ReserveRemaining", "CommissionReserved", "CommissionReserveRemaining", "CommissionPaid", "Price", "Opened", "IsOpen", "Sentinel", "CancelInitiated", "ImmediateOrCancel", "IsConditional")
  implicit val histResult             = jsonFormat(WithdrawalResult, "PaymentUuid", "Currency", "Amount", "Address", "Opened", "Authorized", "PendingPayment", "TxCost", "TxId", "Canceled", "InvalidAddress")
  implicit val depositResult          = jsonFormat(DepositResult, "Id", "Amount", "Currency", "Confirmations", "LastUpdated", "TxId", "CryptoAddress")
  implicit val marketResult           = jsonFormat(MarketResult, "MarketCurrency", "BaseCurrency", "MarketCurrencyLong", "BaseCurrencyLong", "MinTradeSize", "MarketName", "IsActive", "Created")
  implicit val currencyResult         = jsonFormat(CurrencyResult, "Currency", "CurrencyLong", "MinConfirmation", "TxFee", "IsActive", "CoinType")
  implicit val tickerResult           = jsonFormat(TickerResult, "Bid", "Ask", "Last")
  implicit val marketSummaryResult    = jsonFormat(MarketSummaryResult, "MarketName", "High", "Low", "Volume", "Last", "BaseVolume", "TimeStamp", "Bid", "Ask", "OpenBuyOrders", "OpenSellOrders", "PrevDay", "Created")
  implicit val orderBookEntryResult   = jsonFormat(OrderBookEntryResult, "Quantity", "Rate")
  implicit val orderBookResult        = jsonFormat2(OrderBookResult)
  implicit val marketHistoryResult    = jsonFormat(MarketHistoryResult, "Id", "TimeStamp", "Quantity", "Price", "Total", "FillType", "OrderType")

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


