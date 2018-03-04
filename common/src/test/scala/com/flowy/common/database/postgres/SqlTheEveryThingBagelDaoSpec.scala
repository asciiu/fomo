package com.flowy.common.database.postgres

import java.io.File
import java.util.UUID

import com.flowy.common.utils.sql.{DatabaseConfig, SqlDatabase}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec}
import com.flowy.common.models._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class SqlTheEveryThingBagelDaoSpec extends FlatSpec  with BeforeAndAfterAll
  with BeforeAndAfterEach{

  //val confFile = new File("src/test/resources/test.conf")

  lazy val config = new DatabaseConfig {
    override def rootConfig = ConfigFactory.load()
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
    val user = User.withRandomUUID("test@test", "Test", "One", "password", "pepper")
    val userApiKey = UserKey.withRandomUUID(user.id, Exchange.Test, "victoria", "secret", "model test", ApiKeyStatus.Verified)

    val fu1 = bagel.add(user)

    Await.result(fu1, Duration.Inf)

    val fu2 = bagel.userKeyDao.add(userApiKey)

    Await.result(fu2, Duration.Inf)

    //val order = Order.withRandomUUID(user.id, )

    assert(true === true)
  }
}
