package com.softwaremill.bootzooka


import com.flow.utils.cassandra.ConfigConnector
import com.outworkers.phantom.connectors.CassandraConnection
import com.outworkers.phantom.dsl._
import com.softwaremill.bootzooka.passwordreset.domain.PasswordResetCodes
import com.softwaremill.bootzooka.user.domain.Users

class AppDatabase(override val connector: CassandraConnection) extends Database[AppDatabase](connector) {
  object UserDao extends Users with Connector
  object CodeDao extends PasswordResetCodes with Connector
}

object Database extends AppDatabase(ConfigConnector.connector)

