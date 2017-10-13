package com.softwaremill.bootzooka.test

import java.io.File

import com.chrisomeara.pillar.core.{Migrator, Parser, Registry, ReplicationOptions}
import com.datastax.driver.core.Session
import com.flow.utils.cassandra.JarUtils


class Pillar {

  //private val registry = Registry(loadMigrationsFromJarOrFilesystem())
  private val currentDir = new java.io.File(".").getCanonicalPath
  private val file = new File(s"$currentDir/backend/conf/pillar/migrations/fomo/")
  private val registry = Registry.fromDirectory(file)
  private val migrator = Migrator(registry)

  private def loadMigrationsFromJarOrFilesystem() = {
    val migrationsDir = "migrations/fomo/"
    val migrationNames = JarUtils.getResourceListing(getClass, migrationsDir).toList.filter(_.nonEmpty)
    val parser = Parser()

    migrationNames.map(name => getClass.getClassLoader.getResourceAsStream(migrationsDir + name)).map {
      stream =>
        try {
          parser.parse(stream)
        } finally {
          stream.close()
        }
    }.toList
  }

  def initialize(session: Session, keyspace: String, replicationFactor: Int): Unit = {
    migrator.initialize(
      session,
      keyspace,
      new ReplicationOptions(Map("class" -> "SimpleStrategy", "replication_factor" -> replicationFactor))
    )
  }

  def migrate(session: Session): Unit = {
    migrator.migrate(session)
  }

  def destroy(session: Session, keyspace: String): Unit = {
    migrator.destroy(session, keyspace)
  }
}
