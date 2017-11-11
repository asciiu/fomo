package com.flow.marketmaker.database

import com.flow.marketmaker.models.MarketStructures.MarketUpdate

import scala.concurrent.Future

trait MarketUpdateDao {
  def insert(update: List[MarketUpdate]): Future[Option[Int]]
}
