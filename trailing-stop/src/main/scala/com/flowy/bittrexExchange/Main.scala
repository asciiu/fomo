package com.flowy.bittrexExchange

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory


object Main extends App {
  // Override the configuration of the port when specified as program argument
  val port = if (args.isEmpty) "2552" else args(0)
  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
    withFallback(ConfigFactory.parseString("akka.cluster.roles = [TrailingStopLossService]")).
    withFallback(ConfigFactory.load())

  val system = ActorSystem("ClusterSystem", config)
  system.actorOf(Props[TrailingStopLossService], name = "trailing-stop")
}
