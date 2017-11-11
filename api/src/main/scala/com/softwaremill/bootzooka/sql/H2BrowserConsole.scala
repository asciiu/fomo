package com.softwaremill.bootzooka.sql

import com.flowy.marketmaker.common.sql.{DatabaseConfig, SqlDatabase}
import com.typesafe.config.ConfigFactory

object H2BrowserConsole extends App {
  val config = new DatabaseConfig {
    def rootConfig = ConfigFactory.load()
  }

  new Thread(new Runnable {
    def run() = new org.h2.tools.Console().runTool("-url", SqlDatabase.embeddedConnectionStringFromConfig(config))
  }).start()

  println("The console is now running in the background.")
}
