package com.flowy.common.services
import redis.RedisClient

class BalanceService(redis: RedisClient) {

  // when a new user adds their key pull the balances from the exchange and
  // populate the user's internal balance

  // balance layout:
  // exchange
  // currency
  // available
  // exchangeTotal
  // exchangeAvailable
  // pending
  // cryptoAddress

  // when a new trade is posted update the available balance

  // when a trade is deleted update the available balance

  // when a trade is bought update the available balance
  // need to populate the bought balance
}
