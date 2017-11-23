package com.softwaremill.bootzooka.swagger

import com.github.swagger.akka.model.Info
import akka.actor.ActorSystem
import com.flowy.fomoapi.routes.{UsersRoutes, VersionRoutes}
import com.github.swagger.akka._
import com.softwaremill.bootzooka.version.BuildInfo._

class SwaggerDocService(address: String, port: Int, system: ActorSystem) extends SwaggerHttpService {
  override val apiClasses: Set[Class[_]] = Set(classOf[VersionRoutes], classOf[UsersRoutes])

  override val host = address + ":" + port
  override val apiDocsPath = "api-docs"
  override val basePath = "/"
  override val info = Info(
    version = buildDate,
    title = "Fomo",
    description = "All JSON responses will be formatted as: {status: string, message: string, data: { modelName: { ... }}}"
  )
}
