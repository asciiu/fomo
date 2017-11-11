package com.flow.bittrex

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.flow.marketmaker.{MarketEvent, MarketEventBus}
import com.flowy.marketmaker.models.MarketStructures.MarketUpdate

import scala.concurrent.ExecutionContext


object BittrexMarketEventPublisher {

  def props(eventBus: MarketEventBus)(implicit context: ExecutionContext, system: ActorSystem): Props =
    Props(new BittrexMarketEventPublisher(eventBus))

  case class MarketSummaries(summaries: List[BittrexNonce])
  case class MarketDeltas(deltas: List[MarketUpdate])
}

class BittrexMarketEventPublisher(eventBus: MarketEventBus)
                                 (implicit executionContext: ExecutionContext, system: ActorSystem)
  extends Actor with ActorLogging {

  import BittrexMarketEventPublisher._

  def receive = {
    case MarketSummaries(summaries) =>
      publishSummary(summaries)

    case MarketDeltas(deltas) =>
      publishDeltas(deltas)

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

  def publishDeltas(deltas: List[MarketUpdate]) = {
    deltas.foreach{ bittrexMarketUpdate =>
      eventBus.publish(MarketEvent("updates", bittrexMarketUpdate))
    }
  }
}
