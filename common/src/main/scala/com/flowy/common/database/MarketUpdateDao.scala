package com.flowy.common.database

import com.flowy.common.models.MarketStructures.MarketUpdate

import scala.concurrent.Future

trait MarketUpdateDao {
  def insert(update: List[MarketUpdate]): Future[Option[Int]]
  //def insert(update: List[]): Future[Option[Int]]
}
