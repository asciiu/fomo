package com.flowy.cacheService

import java.util.UUID

import akka.pattern.ask
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import akka.util.Timeout
import com.flowy.common.api.Auth
import com.flowy.common.utils.sql.SqlDatabase
import com.flowy.common.database.postgres.SqlTheEverythingBagelDao
import com.flowy.cacheService.CacheService.CacheBittrexWallets
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import redis.RedisClient
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import slick.jdbc.H2Profile.api._


trait ServiceSpecWithDb extends FlatSpecLike
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with ScalaFutures
  with IntegrationPatience {

  private val connectionString = "jdbc:h2:mem:bootzooka_test" + this.getClass.getSimpleName + ";DB_CLOSE_DELAY=-1"
  val sqlDatabase = SqlDatabase.createEmbedded(connectionString)

  override protected def beforeAll() {
    super.beforeAll()
    sqlDatabase.updateSchema()
  }

  override protected def afterAll() {
    super.afterAll()
    sqlDatabase.db.run(sqlu"DROP ALL OBJECTS").futureValue
    sqlDatabase.close()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
  }

  override protected def afterEach() {
    super.afterEach()
  }
}

class CacheServiceSpec(_system: ActorSystem) extends TestKit(_system) with ServiceSpecWithDb {

  def this() = this(ActorSystem("WalletServiceSpec"))

  implicit val executor: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  lazy val redis = new RedisClient()
  lazy val bagel = new SqlTheEverythingBagelDao(sqlDatabase)
  val cacheService = system.actorOf(CacheService.props(bagel, redis))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "A Greeter Actor" should "pass on a greeting message when instructed to" in {
    implicit val timeout = Timeout(2.second)
    val status = (cacheService ? CacheBittrexWallets(UUID.randomUUID(), Auth("secret", "key"))).mapTo[Boolean].futureValue
    status should be(false)
  }
}
