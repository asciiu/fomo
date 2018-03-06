package com.softwaremill.bootzooka.test

import com.typesafe.config.ConfigFactory
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import com.flowy.common.utils.sql.{DatabaseConfig, SqlDatabase}
import slick.jdbc.H2Profile.api._

trait FlatSpecWithDb extends FlatSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with IntegrationPatience {

  private val connectionString = "jdbc:h2:mem:bootzooka_test" + this.getClass.getSimpleName + ";DB_CLOSE_DELAY=-1"
  //val sqlDatabase              = SqlDatabase.createEmbedded(connectionString)
  lazy val config = new DatabaseConfig {
    override def rootConfig = ConfigFactory.load()
  }

  lazy val sqlDatabase = SqlDatabase.create(config)

  override protected def beforeAll() {
    super.beforeAll()
    sqlDatabase.updateSchema()
  }

  override protected def afterAll() {
    super.afterAll()
    //sqlDatabase.db.run(sqlu"DROP ALL OBJECTS").futureValue
    sqlDatabase.clean()
    sqlDatabase.close()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
  }

  override protected def afterEach() {
    super.afterEach()
  }
}
