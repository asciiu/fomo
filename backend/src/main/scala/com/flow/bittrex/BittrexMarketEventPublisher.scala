package com.flow.bittrex

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import scala.concurrent.ExecutionContext


object BittrexMarketEventPublisher {

  def props()(implicit context: ExecutionContext, system: ActorSystem): Props =
    Props(new BittrexMarketEventPublisher())

  case class MarketSummaries(summaries: List[BittrexNonce])
}

class BittrexMarketEventPublisher()(implicit executionContext: ExecutionContext,
                                    system: ActorSystem) extends Actor with ActorLogging {

  import BittrexMarketEventPublisher._
  val eventBus = BittrexEventBus()

  def receive = {
    case MarketSummaries(summaries) =>
      publishSummary(summaries)

    case x =>
      log.warning(s"received unknown $x")
  }

  def publishSummary(summary: List[BittrexNonce]) = {
    summary.foreach{ s =>
      s.Deltas.foreach{ bittrexMarketUpdate =>
        //println(bittrexMarketUpdate)
        eventBus.publish(MarketEvent(BittrexEventBus.Updates, bittrexMarketUpdate))
      }
    }
  }
}
