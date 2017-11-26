package com.softwaremill.bootzooka.sql


import com.flowy.common.utils.sql.{DatabaseConfig, SqlDatabase}
import com.typesafe.config.ConfigFactory
import org.h2.tools.Console

object H2BrowserConsole extends App {
  val config = new DatabaseConfig {
    def rootConfig = ConfigFactory.load()
  }

  new Thread(new Runnable {
    def run() = new Console().runTool("-url", SqlDatabase.embeddedConnectionStringFromConfig(config))
  }).start()

  println("The console is now running in the background.")
}
