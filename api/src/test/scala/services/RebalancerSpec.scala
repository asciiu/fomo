package services

import java.util.UUID

import com.flowy.common.database.postgres.SqlTheEverythingBagelDao
import com.flowy.common.utils.sql.SqlDatabase
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import slick.jdbc.H2Profile.api._


trait ServiceSpecWithDb extends AsyncFlatSpec
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

class BalancerServiceSpec extends ServiceSpecWithDb {

  lazy val bagel = new SqlTheEverythingBagelDao(sqlDatabase)

  "BalancerService" should "be able to access balances" in {
    //implicit val timeout = Timeout(2.second)
    //val status = (cacheService ? CacheBittrexWallets(UUID.randomUUID(), Auth("secret", "key"))).mapTo[Boolean].futureValue
    bagel.findBalance(UUID.randomUUID(), UUID.randomUUID(), "BTC").map { x  =>
      assert(x == None)
    }
  }
}


