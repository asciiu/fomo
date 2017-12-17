package com.flowy.common.services

import com.flowy.common.api.Bittrex.ExchangeBalance
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models.{Balance, Exchange}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}



class BalanceService(bagel: TheEverythingBagelDao)(implicit ec: ExecutionContext) {

  // when a new user adds their key pull the balances from the exchange and
  // populate the user's internal balance
  def populateBalances(userId: UUID, exchange: Exchange.Value, apiKeyId: UUID, exBalances: Seq[ExchangeBalance]): Future[Seq[Balance]] = {
    // convert balances here
    val balances = exBalances.map{ exb =>
      val exchangeAvailable = exb.exchangeAvailableBalance

      Balance(
        id = UUID.randomUUID(),
        userId = userId,
        apiKeyId = apiKeyId,
        exchange = exchange,
        currencyName = exb.currency,
        currencyNameLong = "",
        availableBalance = exchangeAvailable,
        exchangeTotalBalance = exb.exchangeTotalBalance,
        exchangeAvailableBalance = exchangeAvailable,
        pending = Some(exb.pending),
        blockchainAddress = exb.cryptoAddress)
    }

    // Assume all inserted successful?
    // this could cause issues if they do not all succeed
    bagel.insert(balances).map {
      case x if x > 0 => balances
      case _ => Seq.empty[Balance]
    }
  }

  // when a new trade is posted update the available balance
  def updateBalance(balance: Balance) = {
    bagel.updateBalance(balance)
  }

  // when a trade is deleted update the available balance

  // when a trade is bought update the available balance
  // need to populate the bought balance
}
