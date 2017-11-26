package com.flowy.common.database.postgres

import com.flowy.common.utils.sql.SqlDatabase
import com.flowy.common.database.MarketUpdateDao
import com.flowy.common.database.postgres.schema.SqlMarketUpdateSchema
import com.flowy.common.models.MarketStructures.MarketUpdate

import scala.concurrent.{ExecutionContext, Future}

class SqlMarketUpdateDao(protected val database: SqlDatabase)(implicit val ec: ExecutionContext)
  extends MarketUpdateDao with SqlMarketUpdateSchema {

  import database._
  import database.driver.api._

  def insert(updates: List[MarketUpdate]): Future[Option[Int]] = {
    db.run(marketUpdates ++= updates)
  }
}
