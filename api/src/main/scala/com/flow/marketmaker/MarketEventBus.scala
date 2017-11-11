package com.flow.marketmaker

import akka.event.{ActorEventBus, LookupClassification}


case class MarketEvent(topic: String, payload: Any)

/**
  * Created by bishop on 8/16/16.
  */
class MarketEventBus(val exchangeName: String) extends ActorEventBus with LookupClassification {
  type Event = MarketEvent
  type Classifier = String

  protected def mapSize(): Int = 10

  def classify(event: Event): Classifier = {
    event.topic
  }

  def publish(event: Event, subscriber: Subscriber): Unit = {
    subscriber ! event.payload
  }
}
