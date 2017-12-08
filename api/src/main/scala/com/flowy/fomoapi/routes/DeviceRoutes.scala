package com.flowy.fomoapi.routes

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import com.flowy.common.database.TheEverythingBagelDao
import com.flowy.common.models.UserDevice
import com.softwaremill.bootzooka.common.api.RoutesSupport
import com.softwaremill.bootzooka.user.api.SessionSupport
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.auto._
import redis.RedisClient


trait DeviceRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  def bittrexService: ActorRef
  def bagel: TheEverythingBagelDao
  def redis: RedisClient

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
        entity(as[UserDevice]) { userDevice =>
          completeOk
        }
      }
    }
  }

  def updateDevice = {
    path(JavaUUID) { tradeId =>
      put {
        userFromSession { user =>
          entity(as[UserDevice]) { userDevice =>
            completeOk
          }
        }
      }
    }
  }
}
