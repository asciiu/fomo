package com.flowy.fomoapi

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import com.flowy.fomoapi.routes._
import com.softwaremill.bootzooka.ServerConfig
import com.softwaremill.bootzooka.common.api.RoutesRequestWrapper
import com.softwaremill.bootzooka.swagger.SwaggerDocService


trait Routes extends RoutesRequestWrapper
  with ApiKeyRoutes
  with DeviceRoutes
  with UsersRoutes
  with PasswordResetRoutes
  with VersionRoutes
  with TradeRoutes
  with LookupRoutes {

  def system: ActorSystem
  def config: ServerConfig

  lazy val routes = requestWrapper {
    pathPrefix("api") {
      apiKeyRoutes ~
      deviceRoutes ~
      passwordResetRoutes ~
      usersRoutes ~
      versionRoutes ~
      tradeRoutes ~
      marketRoutes
    } ~
    indexRoute ~
    swaggerDocs
  }

  // needed for aws health checks
  def indexRoute =
    path("index.html") {
      get {
        completeOk
      }
    }

  // swagger for api docs
  def swaggerDocs = path("docs") {
    getFromResource("swagger/index.html") } ~
    new SwaggerDocService(config.serverHost, config.serverPort, system).routes ~
    getFromResourceDirectory("swagger")

}
