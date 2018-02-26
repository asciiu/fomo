package com.flowy.binance

import com.flowy.common.utils.sql.SqlDatabase

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class SqlBinanceDao(protected val database: SqlDatabase)(implicit val ec: ExecutionContext)
  extends SqlBinanceTickerSchema {

  import database._
  import database.driver.api._

  def insert(tickers: List[Binance24HrTicker]): Future[Option[Int]] = {
    val ts = tickers.map { t =>
      val frag1 = BinanceTickerFrag1(t.e, t.E, t.s)
      val frag2 = BinanceTickerFrag2(t.p, t.P, t.w, t.x, t.c,
        t.Q, t.b, t.B, t.a, t.A, t.o, t.h, t.l, t.v, t.q, t.O, t.C, t.F, t.L, t.n)

      BinanceTicker(frag1, frag2)
    }

    db.run((binanceTickers ++= ts).asTry)
      .map { result =>
        result match {
          case Success(r) => Some(1)
          case Failure(e) => {
            println(s"SQL Error, ${e.getMessage}")
            None
          }
        }
      }
  }
}

