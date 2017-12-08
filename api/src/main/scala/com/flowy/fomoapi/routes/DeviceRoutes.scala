package com.flowy.fomoapi.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models.UserDevice
import com.flowy.fomoapi.services.UserDeviceService
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.user.api.SessionSupport
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.auto._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import redis.RedisClient


trait DeviceRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  def bagel: TheEverythingBagelDao
  def redis: RedisClient
  def deviceService: UserDeviceService

  // TODO
  // when a trade does not execute successfully you need an error log to tell you why
  val deviceRoutes = logRequestResult("DeviceRoutes") {
    pathPrefix("devices") {
      deleteDevice ~
      getDevice ~
      updateDevice ~
      postDevice ~
      listDevices
    }
  }

  def deleteDevice = {
    path(JavaUUID) { deviceId =>
      delete {
        userFromSession { user =>
          completeOk
          // TODO delete device in DB and in cache
          //complete(JSendResponse(JsonStatus.Success, "", device.asJson))
        }
      }
    }
  }

  def listDevices = {
    get {
      userFromSession { user =>
        completeOk
      }
    }
  }

  def getDevice = {
    path(JavaUUID) { tradeId =>
      get {
        userFromSession { user =>
          completeOk
        }
      }
    }
  }

  def postDevice = {
    post {
      userFromSession { user =>
        entity(as[UserDeviceRequest]) { deviceReq =>
          onSuccess(deviceService.addUserDevice(UserDevice(user.id, deviceReq.deviceType, deviceReq.deviceId, deviceReq.deviceToken))) {
            case Some(device) =>
              complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", device.asJson))
            case None =>
              complete(StatusCodes.Conflict, JSendResponse(JsonStatus.Fail, "", Json.Null))
          }
        }
      }
    }
  }

  def updateDevice = {
    path(JavaUUID) { tradeId =>
      put {
        userFromSession { user =>
          entity(as[UserDeviceRequest]) { userDevice =>
            completeOk
          }
        }
      }
    }
  }
}

case class UserDeviceRequest(deviceType: String, deviceId: String, deviceToken: String)
