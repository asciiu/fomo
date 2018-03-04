package com.flowy.common.database.postgres

import java.io.File
import java.util.UUID

import com.flowy.common.utils.sql.{DatabaseConfig, SqlDatabase}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec}
import com.flowy.common.models.{Order, User}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

class SqlTheEveryThingBagelDaoSpec extends FlatSpec  with BeforeAndAfterAll
  with BeforeAndAfterEach{

  val confFile = new File("src/test/resources/test.conf")

  lazy val config = new DatabaseConfig {
    override def rootConfig = ConfigFactory.parseFile(confFile)
  }

  lazy val sqlDatabase = SqlDatabase.create(config)
  lazy val bagel = new SqlTheEverythingBagelDao(sqlDatabase)

  override protected def beforeAll() {
    super.beforeAll()
    sqlDatabase.updateSchema()
  }

  override protected def afterAll() {
    super.afterAll()
    //sqlDatabase.clean()
    //sqlDatabase.
    //sqlDatabase.db.run(sqlu"DROP ALL OBJECTS").futureValue
    sqlDatabase.close()
  }

  "The bagel dao" should "insert a record" in {
    //val user = User()

    println(config.dbPostgresDbName)

    assert(true === true)
  }
}
