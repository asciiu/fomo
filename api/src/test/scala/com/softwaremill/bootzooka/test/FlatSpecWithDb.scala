package com.softwaremill.bootzooka.test

import com.flowy.marketmaker.common.sql.SqlDatabase
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import slick.jdbc.H2Profile.api._

trait FlatSpecWithDb
    extends FlatSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with IntegrationPatience {

  private val connectionString = "jdbc:h2:mem:bootzooka_test" + this.getClass.getSimpleName + ";DB_CLOSE_DELAY=-1"
  val sqlDatabase              = SqlDatabase.createEmbedded(connectionString)

  override protected def beforeAll() {
    super.beforeAll()
  }

  override protected def afterAll() {
    super.afterAll()
    sqlDatabase.close()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    sqlDatabase.updateSchema()
  }

  override protected def afterEach() {
    super.afterEach()
    sqlDatabase.db.run(sqlu"DROP ALL OBJECTS").futureValue
  }
}
