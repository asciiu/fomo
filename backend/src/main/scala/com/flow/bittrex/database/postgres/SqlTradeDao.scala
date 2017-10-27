package com.flow.bittrex.database.postgres

import com.flow.bittrex.database.TradeDao
import com.flow.bittrex.database.postgres.schema.SqlBittrexTrade
import com.flow.bittrex.models.BittrexTrade
import com.softwaremill.bootzooka.common.sql.SqlDatabase
import scala.concurrent.{ExecutionContext, Future}


class SqlTradeDao(protected val database: SqlDatabase)(implicit val ec: ExecutionContext)
  extends TradeDao with SqlBittrexTrade {

  import database._
  import database.driver.api._

  def insert(trade: BittrexTrade): Future[Int] = {
    db.run(bittrexTrades += trade)
  }
}
