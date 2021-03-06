package com.flowy.bexchange

import java.time.{Instant, ZoneOffset}
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import com.flowy.bexchange.trade.TradeActor
import com.flowy.bexchange.trade.TradeActor.Update
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models.MarketStructures.MarketUpdate
import com.flowy.common.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import redis.RedisClient

import scala.tools.reflect.ToolBox
import scala.util.{Failure, Success}


object MarketTradeService {
  def props(marketName: String, bagel: TheEverythingBagelDao, redis: RedisClient)(implicit context: ExecutionContext) =
    Props(new MarketTradeService(marketName, bagel, redis))

  case class DeleteTrade(trade: Trade, senderOpt: Option[ActorRef] = None)
  case class PostTrade(forUser: UserData, request: TradeRequest, senderOpt: Option[ActorRef] = None)
  case class UpdateTrade(forUser: UserData, tradeId: UUID, request: TradeRequest, senderOpt: Option[ActorRef] = None)
}


/**
  * Services a single market: example BTC-NEO
  * @param marketName
  * @param bagel
  * @param redis
  */
class MarketTradeService(val marketName: String, bagel: TheEverythingBagelDao, redis: RedisClient) extends Actor
  with ActorLogging {

  import MarketTradeService._
  import trade.TradeActor.Cancel
  import scala.reflect.runtime.currentMirror

  implicit val akkaSystem = context.system

  // maps id of trade to the trade actor
  val trades = collection.mutable.Map[UUID, ActorRef]()

  val dynamic = currentMirror.mkToolBox()

  val mediator = DistributedPubSub(context.system).mediator


  override def preStart() = {
    // load pending conditions from bagel
    bagel.findTradesByStatus(marketName, Seq(TradeStatus.Pending, TradeStatus.Bought)).map { pendingTrades =>
      pendingTrades.foreach { trade =>
        trades += trade.id -> context.actorOf(TradeActor.props(trade, bagel), trade.id.toString)
      }
    }

    log.info(s"$marketName actor started")
  }

  def receive: Receive = {
    case update: MarketUpdate =>
      // forward to all child actors of me
      context.system.actorSelection(s"${self.path}/*") ! update

    case PostTrade(user, request, Some(sender)) =>
      postTrade(user, request, sender)

    case UpdateTrade(user, tradeId, request, Some(sender)) =>
      updateTrade(user, tradeId, request, sender)

    case DeleteTrade(trade, Some(sender)) =>
      deleteTrade(trade, sender)

    case x =>
      log.warning(s"received unknown message - $x")
  }


  /**
    * Add trade to DB and insert buy conditions.
    * @param user
    * @param request
    * @param senderRef response to sender when finished with Some(trade) or None
    * @return
    */
  private def postTrade(user: UserData, request: TradeRequest, senderRef: ActorRef) = {
    val trade = Trade.fromRequest(request, user.id)

    log.info(s"MarketTradeService.postTrade - $request")

    val stuff = for {
      result <- bagel.insert(trade)
      balance <- bagel.findBalance(user.id, trade.apiKeyId, trade.info.baseCurrency)
    } yield (result, balance)

    stuff.onComplete {
      // trade insert result willl be > 0 for success
      // and balance must be > base quantity
      case Success((result, Some(balance))) if (result > 0 && balance.availableBalance > trade.baseQuantity) =>
        val newBalance = balance.copy(availableBalance = balance.availableBalance - trade.baseQuantity)
        bagel.updateBalance(newBalance)

        // start new process to watch this trade
        trades += trade.id -> context.actorOf(TradeActor.props(trade, bagel), trade.id.toString)

        // send trade response to sender
        senderRef ! Some(trade)
      case _ =>
        senderRef ! None
    }
  }

  private def deleteTrade(trade: Trade, sender: ActorRef) = {
    if (trades.contains(trade.id)) {
      trades(trade.id) ! Cancel(sender)
    } else {
      sender ! None
    }
  }

  private def updateTrade(user: UserData, tradeId: UUID, request: TradeRequest, sender: ActorRef) = {
    if (trades.contains(tradeId)) {
      trades(tradeId) ! Update(user, request, sender)
    } else {
      sender ! None
    }
  }
}


