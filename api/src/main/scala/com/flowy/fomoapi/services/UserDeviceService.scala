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
    bagel.findUserDevice(userDevice.userId, userDevice.deviceId).flatMap {
      case Some(device) =>
        bagel.updateDevice(device.copy(deviceType = device.deviceType, deviceToken = device.deviceToken))
      case None =>
        bagel.insert(userDevice).map {
          case 1 => Some(userDevice)
          case _ => None
        }
    }
  }

  def update(userDevice: UserDevice): Future[Option[UserDevice]] = {
    bagel.updateDevice(userDevice)
  }

  def remove(userId: UUID, deviceId: UUID): Future[Option[UserDevice]] = {
    // TODDO when remove a key pause all trades with this key
    getUserDevice(userId, deviceId).flatMap {
      case Some(device) => bagel.deleteDevice(device)
      case None => Future.successful(None)
    }
  }

  def getUserDevice(userId: UUID, deviceId: UUID): Future[Option[UserDevice]] = {
    bagel.findUserDevice(userId, deviceId)
  }

  def getUserDevices(userId: UUID): Future[Seq[UserDevice]] = {
    bagel.findUserDevices(userId)
  }
}
