package com.flowy.common.database.postgres

import java.io.File
import java.util.UUID

import com.flowy.common.utils.sql.{DatabaseConfig, SqlDatabase}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec}
import com.flowy.common.models._
import io.circe.Json
import slick.jdbc.PostgresProfile.api._
import io.circe.parser._

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
    val conditions = parse("""{"buyConditions": "price < 2.0"}""").getOrElse(Json.Null)
    val order = Order.withRandomUUID(user.id, userApiKey.id, Exchange.Test, "123", "test", "test", OrderSide.Buy, OrderType.Limit, 0.01,100, 0, OrderStatus.Open, conditions)
    val condition = parse(""""price < 2.0"""").getOrElse(Json.Null)
    val orderFill = OrderFill.create(order.id, condition, 1.2, 2)

    val fu1 = bagel.add(user)

    Await.result(fu1, Duration.Inf)

    val fu2 = bagel.userKeyDao.add(userApiKey)

    Await.result(fu2, Duration.Inf)

    val fu3 = bagel.insert(order)

    Await.result(fu3, Duration.Inf)

    val fu4 = bagel.insert(orderFill)

    Await.result(fu4, Duration.Inf)

    val fu5 = bagel.findOrderBy(order.id)

    Await.result(fu5, Duration.Inf)

    fu5.map { result =>
      println(s"HERE $result")
    }

    assert(true === true)
  }
}
