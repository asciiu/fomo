package database

import com.flow.utils.cassandra.ConfigConnector
import com.outworkers.phantom.connectors.CassandraConnection
import com.outworkers.phantom.dsl._
import com.softwaremill.bootzooka.passwordreset.domain.PasswordResetCodes
import database.cassandra.tables.{UsersByEmail, UsersById}

class CassandraDatabase(override val connector: CassandraConnection)
  extends Database[CassandraDatabase](connector) {

  object UsersByIdDao extends UsersById with Connector
  object UsersByEmailDao extends UsersByEmail with Connector
  object CodeDao extends PasswordResetCodes with Connector
}

object AppDatabase extends CassandraDatabase(ConfigConnector.connector)

