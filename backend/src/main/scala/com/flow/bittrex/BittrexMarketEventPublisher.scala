package com.flow.bittrex

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.flow.marketmaker.{MarketEvent, MarketEventBus}

import scala.concurrent.ExecutionContext


object BittrexMarketEventPublisher {

  def props(eventBus: MarketEventBus)(implicit context: ExecutionContext, system: ActorSystem): Props =
    Props(new BittrexMarketEventPublisher(eventBus))

  case class MarketSummaries(summaries: List[BittrexNonce])
}

class BittrexMarketEventPublisher(eventBus: MarketEventBus)
                                 (implicit executionContext: ExecutionContext, system: ActorSystem)
  extends Actor with ActorLogging {

  import BittrexMarketEventPublisher._

  def receive = {
    case MarketSummaries(summaries) =>
      publishSummary(summaries)

    case x =>
      log.warning(s"received unknown $x")
  }

  def publishSummary(summary: List[BittrexNonce]) = {
    summary.foreach{ s =>
      s.Deltas.foreach{ bittrexMarketUpdate =>
        eventBus.publish(MarketEvent("updates", bittrexMarketUpdate))
      }
    }
  }
}
