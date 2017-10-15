package com.softwaremill.bootzooka.swagger

import com.github.swagger.akka.model.Info

import scala.reflect.runtime.{universe => ua}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.swagger.akka._
import com.softwaremill.bootzooka.version.BuildInfo._
import routes.{UsersRoutes, VersionRoutes}
import services.UserService

class SwaggerDocService(address: String, port: Int, system: ActorSystem) extends SwaggerHttpService {
  override val apiClasses: Set[Class[_]] = Set(classOf[VersionRoutes], classOf[UsersRoutes])

  override val host = address + ":" + port
  override val info = Info(version = buildDate, title = "Fomo")
  override val apiDocsPath = "api-docs"
  override val basePath = "/"
}
