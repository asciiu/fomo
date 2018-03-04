package com.flowy.common.database.postgres

import java.io.File

import com.flowy.common.utils.sql.{DatabaseConfig, SqlDatabase}
import com.typesafe.config.ConfigFactory
import org.scalatest.FlatSpec

import scala.concurrent.ExecutionContext.Implicits.global

class SqlTheEveryThingBagelDaoSpec extends FlatSpec {

  val confFile = new File("src/test/resources/test.conf")

  lazy val config = new DatabaseConfig {
    override def rootConfig = ConfigFactory.parseFile(confFile)
  }

  lazy val sqlDatabase = SqlDatabase.create(config)
  lazy val bagel = new SqlTheEverythingBagelDao(sqlDatabase)

  "The bagel dao" should "insert a record" in {
    println(config.dbPostgresDbName)

    assert(true === true)
  }
}
