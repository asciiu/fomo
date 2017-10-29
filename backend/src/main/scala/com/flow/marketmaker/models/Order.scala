package com.flow.marketmaker.models

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID


object OrderStatus extends Enumeration {
  val Pending    = Value("pending")
  val Completed  = Value("completed")
}


object OrderType extends Enumeration {
  val Buy = Value("buy")
  val Sell    = Value("sell")
}


// this gets posted from the client
// move this to the route file
case class BuyCondition(conditionType: String, indicator: String, operator: String, value: Double)
case class BuyOrder(exchangeName: String,
                    marketName: String,
                    quantity: Double,
                    buyConditions: List[BuyCondition])


case class Order(id: Option[Long] = None,
                 userId: UUID,
                 exchangeName: String,
                 marketName: String,
                 createdTime: OffsetDateTime,
                 completedTime: Option[OffsetDateTime] = None,
                 completedCondition: Option[String] = None,
                 priceActual: Option[Double] = None,
                 quantity: Double,
                 orderType: OrderType.Value,
                 status: OrderStatus.Value,
                 orConditions: List[Conditional]) {

  // evaluates all conditions and returns true if any of the conditions are true
  def isCondition(test: Any): Boolean = orConditions.exists( _.evaluate(test) )
  def getCondition(test: Any): Option[String] = orConditions.find( _.evaluate(test) ).map (_.toString)
}

object Order {
  def fromBuyOrder(buyOrder: BuyOrder, forUserId: UUID): Order = {

    val conditions = buyOrder.buyConditions.map { c =>

      c.conditionType match {
        case "simpleConditional" => SimpleConditionalFactory.makeCondition(c.operator, c.value)
        case _ => NullCondition
      }
    }.asInstanceOf[List[Conditional]]

    Order(None, forUserId, buyOrder.exchangeName, buyOrder.marketName,
      Instant.now().atOffset(ZoneOffset.UTC), None, None, None, buyOrder.quantity,
      OrderType.Buy, OrderStatus.Pending, conditions)
  }
}
