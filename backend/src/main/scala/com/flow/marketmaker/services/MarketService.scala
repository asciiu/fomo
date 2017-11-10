package com.flow.marketmaker.services

import java.time.{Instant, ZoneOffset}
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.flow.marketmaker.database.TheEverythingBagelDao
import com.flow.marketmaker.models.MarketStructures.MarketUpdate
import com.flow.marketmaker.models._
import models.BasicUserData

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import redis.RedisClient
import scala.tools.reflect.ToolBox


object MarketService {
  def props(marketName: String, bagel: TheEverythingBagelDao, redis: RedisClient)(implicit context: ExecutionContext) =
    Props(new MarketService(marketName, bagel, redis))

  case class PostTrade(forUser: BasicUserData, request: TradeRequest, sender: Option[ActorRef] = None)
  case class UpdateTrade(forUser: BasicUserData, request: TradeRequest, sender: Option[ActorRef] = None)
}


/**
  * TODO need to rename this to the SimpleCondtionalService
  * note: This service shall be started when the system starts
  * currently it is started when an update in the supervisor gets a market update
  * @param marketName
  * @param bagel
  * @param redis
  */
class MarketService(val marketName: String, bagel: TheEverythingBagelDao, redis: RedisClient) extends Actor
  with ActorLogging {

  import MarketService._
  import scala.reflect.runtime.currentMirror

  implicit val akkaSystem = context.system

  case class TradeBuyCondition(tradeId: UUID, conditionEx: String)

  val buyConditions = collection.mutable.ListBuffer[TradeBuyCondition]()

  val dynamic = currentMirror.mkToolBox()

  override def preStart() = {
    // load pending conditions from bagel
    bagel.findTradesByStatus(marketName, TradeStatus.Pending).map { pendingTrades =>
      if (pendingTrades.length > 0) {
        // map all the conditions into a single collection
        val pendingConditions = pendingTrades.map( t => TradeBuyCondition(t.id, t.buyConditions) )

        log.info(s"$marketName loading pending trades")
        buyConditions.append(pendingConditions: _*)
      }
    }

    log.info(s"$marketName actor started")
  }

  def receive: Receive = {
    case update: MarketUpdate =>
      updateState(update)

    case PostTrade(user, request, Some(sender)) =>
      postTrade(user, request, sender)

    case UpdateTrade(user, request, Some(sender)) =>
      updateTrade(user, request, sender)
  }

  /**
    * Updates the internal state of this service.
    * @param update
    * @return
    */
  private def updateState(update: MarketUpdate) = {
    val lastPrice = update.Last

    val conditionsThatPass = buyConditions.filter { cond =>
      val condition = cond.conditionEx.replace("price", lastPrice.toString)
      dynamic.eval(dynamic.parse(s"$condition")) == true
    }

    conditionsThatPass.foreach { condish =>
      val tradeId = condish.tradeId

      bagel.findTradeById(tradeId).map { t =>

        if (t.isDefined) {
          val trade = t.get
          // TODO this is where the order shall be executed via the BittrexClient

          val updatedTrade = t.get.copy(
            // TODO the buyPrice should be the actual price you may need to read this from bittrex
            buyPrice = Some(lastPrice),
            buyTime = Some(Instant.now().atOffset(ZoneOffset.UTC)),
            status = TradeStatus.Bought
          )

          println(s"Last Price: ${lastPrice}")
          println(s"Execute buy ${trade.quantity} ${trade.marketName} for user: ${trade.userId}")
          bagel.updateTrade(updatedTrade)
        }
      }
    }

    // remove the conditions that have passed
    buyConditions --= conditionsThatPass
  }


  private def postTrade(user: BasicUserData, request: TradeRequest, sender: ActorRef) = {
    val trade = Trade.fromRequest(request, user.id)

    bagel.insert(trade).map { result =>
      if (result > 0) {
        val conditions = trade.buyConditions
        buyConditions.append(TradeBuyCondition(trade.id, conditions))

        sender ! true
      } else {
        sender ! false
      }
    }
  }

  private def updateTrade(user: BasicUserData, request: TradeRequest, sender: ActorRef) = {
    val trade = Trade.fromRequest(request, user.id)
  }
}

