package com.flowy.marketmaker.database.postgres

import com.flowy.marketmaker.common.sql.SqlDatabase
import com.flowy.marketmaker.database.MarketUpdateDao
import com.flowy.marketmaker.database.postgres.schema.SqlMarketUpdateSchema
import com.flowy.marketmaker.models.MarketStructures.MarketUpdate

import scala.concurrent.{ExecutionContext, Future}

class SqlMarketUpdateDao(protected val database: SqlDatabase)(implicit val ec: ExecutionContext)
  extends MarketUpdateDao with SqlMarketUpdateSchema {

  import database._
  import database.driver.api._

  def insert(updates: List[MarketUpdate]): Future[Option[Int]] = {
    db.run(marketUpdates ++= updates)
  }
}
