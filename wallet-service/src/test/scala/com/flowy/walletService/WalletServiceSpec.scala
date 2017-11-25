package com.flowy.walletService

import org.scalatest._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.flowy.marketmaker.common.sql.SqlDatabase
import com.flowy.marketmaker.database.postgres.SqlTheEverythingBagelDao
import com.flowy.walletService.Main.config
import com.flowy.walletService.WalletService.Hello
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import redis.RedisClient

import scala.concurrent.ExecutionContext


trait ServiceSpecWithDb extends FlatSpec
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

class WalletServiceSpec(_system: ActorSystem)
  extends TestKit(_system) with ServiceSpecWithDb {

  def this() = this(ActorSystem("WalletServiceSpec"))

  implicit val executor: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  lazy val redis = new RedisClient()
  lazy val bagel = new SqlTheEverythingBagelDao(sqlDatabase)
  val walletService = system.actorOf(WalletService.props(bagel, redis))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "A Greeter Actor" should "pass on a greeting message when instructed to" in {
    walletService ! Hello
    true should be(true)
  }
}
