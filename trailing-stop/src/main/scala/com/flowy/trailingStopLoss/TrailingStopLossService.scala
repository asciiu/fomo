package com.flowy.trailingStopLoss

import language.postfixOps
import akka.actor.{Actor, ActorLogging, RootActorPath}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.Member
import akka.cluster.MemberStatus
import com.flowy.marketmaker.models.MarketStructures.MarketUpdate
import com.flowy.marketmaker.models.TrailingStopLossRegistration


class TrailingStopLossService extends Actor with ActorLogging{

  val cluster = Cluster(context.system)

  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case update: MarketUpdate =>
      println(update)

    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register

    case MemberUp(m) =>
      register(m)
  }

  def register(member: Member): Unit = {
    if (member.hasRole("api")) {
      log.info("Member is Up: {}", member.address)
      context.actorSelection(RootActorPath(member.address) / "user" / "bittrex") ! TrailingStopLossRegistration
    }
  }
}

