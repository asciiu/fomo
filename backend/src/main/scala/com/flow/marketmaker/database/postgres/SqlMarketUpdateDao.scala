package com.flow.marketmaker.database.postgres

import com.flow.marketmaker.database.MarketUpdateDao
import com.flow.marketmaker.database.postgres.schema.SqlMarketUpdateSchema
import com.flow.marketmaker.models.MarketStructures.MarketUpdate
import com.softwaremill.bootzooka.common.sql.SqlDatabase
import scala.concurrent.{ExecutionContext, Future}

class SqlMarketUpdateDao(protected val database: SqlDatabase)(implicit val ec: ExecutionContext)
  extends MarketUpdateDao with SqlMarketUpdateSchema {

  import database._
  import database.driver.api._

  def insert(updates: List[MarketUpdate]): Future[Option[Int]] = {
    db.run(marketUpdates ++= updates)
  }
}
