package com.flowy.marketmaker.database

import com.flowy.marketmaker.models.MarketStructures.MarketUpdate

import scala.concurrent.Future

trait MarketUpdateDao {
  def insert(update: List[MarketUpdate]): Future[Option[Int]]
}
