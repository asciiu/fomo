package com.flowy.fomoapi.services

import java.util.UUID

import akka.actor.ActorSystem
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.stream.ActorMaterializer
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models.UserDevice

import scala.concurrent.{ExecutionContext, Future}

class UserDeviceService(bagel: TheEverythingBagelDao)(implicit system: ActorSystem, ec: ExecutionContext, materializer: ActorMaterializer) {

  lazy val mediator = DistributedPubSub(system).mediator

  def addUserDevice(userDevice: UserDevice): Future[Option[UserDevice]] = {
    bagel.insert(userDevice).map {
      case 1 => Some(userDevice)
      case _ => None
    }
  }

  //def update(ukey: UserKey): Future[Option[UserKey]] = {
  //}

  //def remove(userId: UUID, keyId: UUID): Future[Boolean] = {
  //}

  //def getUserKey(userId: UUID, keyId: UUID): Future[Option[UserKey]] = {
  //  userKeyDao.findByUserId(userId, keyId)
  //}

  //def getAllKeys(userId: UUID): Future[Seq[UserKey]] = {
  //  userKeyDao.findByUserId(userId)
  //}
}
