package com.softwaremill.bootzooka.test

import com.flow.utils.cassandra.{CassandraConnectionUri, ConfigConnector, Helper}
import com.outworkers.phantom.connectors.{CassandraConnection, ContactPoints}
import com.softwaremill.bootzooka.common.sql.SqlDatabase
import com.typesafe.config.ConfigFactory
import database.CassandraDatabase
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

import scala.collection.JavaConverters._

trait FlatSpecWithDb
    extends FlatSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with IntegrationPatience {

  //private val connectionString = "jdbc:h2:mem:bootzooka_test" + this.getClass.getSimpleName + ";DB_CLOSE_DELAY=-1"
  //val sqlDatabase              = SqlDatabase.createEmbedded(connectionString)

  object TestConnector {
    private val config = ConfigFactory.load()
    private val hosts = config.getStringList("cassandra.host").asScala
    private val port = config.getInt("cassandra.port")
    lazy val keyspace = config.getString("cassandra.testspace")
    lazy val uri = CassandraConnectionUri(s"cassandra://localhost:$port/$keyspace")
    lazy val connector: CassandraConnection = ContactPoints(hosts, port).keySpace(keyspace)
  }


  val uri = CassandraConnectionUri("cassandra://localhost:9042/fomo_test")
  val session = Helper.createSessionAndInitKeyspace(uri)
  val cqlDatabase = new CassandraDatabase(TestConnector.connector)

  //val session = cqlDatabase.connector.session
  val pillar = new Pillar()
  pillar.initialize(session, TestConnector.keyspace, 1)

  override protected def beforeAll() {
    super.beforeAll()
    //pillar.initialize(session, TestConnector.keyspace, 1)
    pillar.migrate(session)
    createAll()
  }

  def clearData() {
    dropAll()
    createAll()
  }

  override protected def afterAll() {
    super.afterAll()
    pillar.destroy(session, TestConnector.keyspace)
    dropAll()
    //sqlDatabase.close()
  }

  private def dropAll() {
    //pillar.destroy(session, TestConnector.keyspace)
    //import sqlDatabase.driver.api._
    //sqlDatabase.db.run(sqlu"DROP ALL OBJECTS").futureValue
  }

  private def createAll() {
    //pillar.initialize(session, TestConnector.keyspace, 1)
    //pillar.migrate(session)
    //pillar.migrate(cqlDatabase.session)
    //sqlDatabase.updateSchema()
  }

  override protected def afterEach() {
    try {
      clearData()
    } catch {
      case e: Exception => e.printStackTrace()
    }

    super.afterEach()
  }
}
