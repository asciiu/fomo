package com.flowy.fomoapi.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models.UserDevice
import com.flowy.fomoapi.services.UserDeviceService
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.user.api.SessionSupport
import com.typesafe.scalalogging.StrictLogging
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
      getUserDevice ~
      updateDevice ~
      postDevice ~
      listDevices
    }
  }

  def deleteDevice = {
    path(JavaUUID) { deviceId =>
      delete {
        userFromSession { user =>
          onSuccess(deviceService.remove(user.id, deviceId)) {
            case Some(device) =>
              complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", device.asJson))
            case None =>
              complete(StatusCodes.NotFound, JSendResponse(JsonStatus.Fail, "cannot find device", Json.Null))
          }
        }
      }
    }
  }

  def listDevices = {
    get {
      userFromSession { user =>
        onSuccess(deviceService.getUserDevices(user.id)) { devices =>
          complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", devices.asJson))
        }
      }
    }
  }

  def getUserDevice = {
    path(JavaUUID) { deviceId =>
      get {
        userFromSession { user =>
          onSuccess(deviceService.getUserDevice(user.id, deviceId)) {
            case Some(device) =>
              complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", device.asJson))
            case None =>
              complete(StatusCodes.NotFound, JSendResponse(JsonStatus.Fail, "not found", Json.Null))
          }
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
    path(JavaUUID) { deviceId =>
      put {
        userFromSession { user =>
          entity(as[UserDeviceRequest]) { deviceReq =>
            onSuccess(deviceService.update(UserDevice(deviceId, user.id, deviceReq.deviceType, deviceReq.deviceId, deviceReq.deviceToken))) {
              case Some(device) =>
                complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", device.asJson))
              case None =>
                complete(StatusCodes.Conflict, JSendResponse(JsonStatus.Fail, "", Json.Null))
            }
          }
        }
      }
    }
  }
}

case class UserDeviceRequest(deviceType: String, deviceId: String, deviceToken: String)
