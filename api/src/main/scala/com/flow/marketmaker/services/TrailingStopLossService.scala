package com.flow.marketmaker.services

import akka.actor.{Actor, ActorLogging, Props}
import com.flow.marketmaker.database.TheEverythingBagelDao
import redis.RedisClient

import scala.concurrent.ExecutionContext

object TrailingStopLossService {
  def props(marketName: String, bagel: TheEverythingBagelDao, redis: RedisClient)(implicit context: ExecutionContext) =
    Props(new TrailingStopLossService(marketName, bagel, redis))
}

class TrailingStopLossService(val marketName: String, bagel: TheEverythingBagelDao, redis: RedisClient) extends Actor
    with ActorLogging {

  def receive: Receive = {
    case x =>
  }
}
