package com.flowy.fomoapi.routes


import java.time.OffsetDateTime
import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.pattern.ask
import akka.util.Timeout
import com.flowy.bexchange.ExchangeService.GetMarkets
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models._
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.user.api.SessionSupport
import com.typesafe.scalalogging.StrictLogging
import io.circe.{Encoder, Json}
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait OrderRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  def bittrexService: ActorRef
  def bagel: TheEverythingBagelDao

  // TODO
  // when a trade does not execute successfully you need an error log to tell you why
  val orderRoutes = logRequestResult("OrderRoutes") {
    pathPrefix("orders") {
      deleteOrder ~
      getOrder ~
      updateOrder ~
      postOrder ~
      listOrders
    }
  }

  implicit val encodeOrder: Encoder[Order] = new Encoder[Order] {
    final def apply(order: Order): Json = {
      Json.obj(
        ("id", Json.fromString(order.id.toString)),
        ("apiKeyId", Json.fromString(order.apiKeyId.toString)),
        ("exchangeName", Json.fromString(order.exchangeName.toString)),
        ("marketName", Json.fromString(order.marketName)),
        ("side", Json.fromString(order.side.toString)),
        ("otype", Json.fromString(order.otype.toString)),
        ("price", Json.fromBigDecimal(order.price)),
        ("quantity", Json.fromBigDecimal(order.qty)),
        ("quantityRemaining", Json.fromBigDecimal(order.qtyRemaining)),
        ("status", Json.fromString(order.status.toString)),
        ("conditions", order.conditions),
        ("createdOn", Json.fromString(order.createdOn.toString))
      )
    }
  }

  def deleteOrder = {
    path(JavaUUID) { orderId =>
      delete {
        userFromSession { user =>
          val now = OffsetDateTime.now()
          val testOrder = Order(
            orderId,
            user.id,
            UUID.randomUUID(),
            Exchange.Test,
            "externalOrderId",
            "externalMarketName",
            "marketName",
            OrderSide.Buy,
            OrderType.Market,
            BigDecimal(0.01),
            BigDecimal(100),
            BigDecimal(0),
            OrderStatus.Pending,
            Json.Null,
            now,
            now
          )
          complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", testOrder.asJson))
        }
      }
    }
  }

  def listOrders = {
    get {
      parameters('marketName.?, 'exchangeName.?, 'status.*) { (marketName, exchangeName, statusIter) =>
        userFromSession { user =>
          val now = OffsetDateTime.now()

          val orders = for (i <- 1 to 3) yield {
            Order(
              UUID.randomUUID(),
              user.id,
              UUID.randomUUID(),
              Exchange.Test,
              "externalOrderId",
              "externalMarketName",
              "marketName",
              OrderSide.Buy,
              OrderType.Market,
              BigDecimal(0.01),
              BigDecimal(100),
              BigDecimal(0),
              OrderStatus.Pending,
              Json.Null,
              now,
              now
            )
          }

          complete(JSendResponse(JsonStatus.Success, "", orders.toList.asJson))
        }
      }
    }
 }

  def getOrder = {
    path(JavaUUID) { orderId =>
      get {
        userFromSession { user =>
          val now = OffsetDateTime.now()
          val testOrder = Order(
            orderId,
            user.id,
            UUID.randomUUID(),
            Exchange.Test,
            "externalOrderId",
            "externalMarketName",
            "marketName",
            OrderSide.Buy,
            OrderType.Market,
            BigDecimal(0.01),
            BigDecimal(100),
            BigDecimal(0),
            OrderStatus.Pending,
            Json.Null,
            now,
            now
          )
          complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", testOrder.asJson))
        }
      }
    }
  }

  def postOrder = {
    post {
      userFromSession { user =>
        entity(as[OrderRequest]) { orderRequest: OrderRequest =>
          implicit val timeout = Timeout(1.second)

          Try(UUID.fromString(orderRequest.apiKeyId)) match {

            case Success(apiKeyId) =>

              // key needs to exist before submitting an order request
              onSuccess(bagel.userKeyDao.findByUserId(user.id, apiKeyId)) {
                case Some(key) =>
                  // send order request
                  println(orderRequest)
                  // send key with order request to processor
                  println(key)

                  // send back a mock order for now
                  val order = Order.create(
                    user.id,
                    apiKeyId,
                    key.exchange,
                    "externalOrderId",
                    "externalMarketName",
                    orderRequest.marketName,
                    OrderSide.withName(orderRequest.side),
                    OrderType.withName(orderRequest.otype),
                    orderRequest.price,
                    orderRequest.qty,
                    0,
                    OrderStatus.Pending,
                    orderRequest.conditions)

                    complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", order.asJson))
                case None =>
                  complete(StatusCodes.UnprocessableEntity, JSendResponse(JsonStatus.Fail, s"the api key was either not found or is not longer verified", Json.Null))
              }

            case Failure(_) =>
              complete(StatusCodes.UnprocessableEntity, JSendResponse(JsonStatus.Fail, s"invalid api key", Json.Null))
          }
        }
      }
    }
  }

  def updateOrder = {
    path(JavaUUID) { orderId =>
      put {
        userFromSession { user =>
          entity(as[OrderRequest]) { orderRequest =>
            val now = OffsetDateTime.now()
            val testOrder = Order(
              orderId,
              user.id,
              UUID.randomUUID(),
              Exchange.Test,
              "externalOrderId",
              "externalMarketName",
              orderRequest.marketName,
              OrderSide.Buy,
              OrderType.Market,
              BigDecimal(0.01),
              BigDecimal(100),
              BigDecimal(0),
              OrderStatus.Pending,
              Json.Null,
              now,
              now
            )
            complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", testOrder.asJson))
          }
        }
      }
    }
  }
}
