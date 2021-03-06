package com.flowy.cache

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.flowy.common.utils.ServerConfig
import com.flowy.common.utils.sql.{DatabaseConfig, SqlDatabase}
import com.flowy.common.database.postgres.SqlTheEverythingBagelDao
import com.typesafe.config.ConfigFactory
import redis.RedisClient

import scala.concurrent.ExecutionContext


object Main extends App {

  val port = if (args.isEmpty) "2555" else args(0)

  lazy val config = new DatabaseConfig with ServerConfig {
    override def rootConfig = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [cache]")).
      withFallback(ConfigFactory.load())
  }

  implicit val system = ActorSystem("cluster", config.rootConfig)
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  lazy val redis = new RedisClient()
  lazy val sqlDatabase = SqlDatabase.create(config)
  lazy val bagel = new SqlTheEverythingBagelDao(sqlDatabase)

  system.actorOf(CacheService.props(bagel, redis), name = "cache")
}
