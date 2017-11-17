package com.flowy.fomoApi

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import com.flowy.fomoApi.routes.{PasswordResetRoutes, TradeRoutes, UsersRoutes, VersionRoutes}
import com.softwaremill.bootzooka.ServerConfig
import com.softwaremill.bootzooka.common.api.RoutesRequestWrapper
import com.softwaremill.bootzooka.swagger.SwaggerDocService

trait Routes extends RoutesRequestWrapper
  with UsersRoutes
  with PasswordResetRoutes
  with VersionRoutes
  with TradeRoutes {

  def system: ActorSystem
  def config: ServerConfig

  lazy val routes = requestWrapper {
    pathPrefix("api") {
      passwordResetRoutes ~
      usersRoutes ~
      versionRoutes ~
      tradeRoutes
    } ~
    swaggerDocs
  }

  // swagger for api docs
  def swaggerDocs = path("docs") {
    getFromResource("swagger/index.html") } ~
    new SwaggerDocService(config.serverHost, config.serverPort, system).routes ~
    getFromResourceDirectory("swagger")
}