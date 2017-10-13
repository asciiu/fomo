import java.text.SimpleDateFormat
import java.util.Date

import sbt.{file, _}
import Keys._

import scala.util.Try
import complete.DefaultParsers._

val slf4jVersion        = "1.7.21"
val logBackVersion      = "1.1.7"
val scalaLoggingVersion = "3.5.0"
val slickVersion        = "3.2.0"
val seleniumVersion     = "2.53.0"
val circeVersion        = "0.7.0"
val akkaVersion         = "2.5.0"
val akkaHttpVersion     = "10.0.4"

val slf4jApi       = "org.slf4j" % "slf4j-api" % slf4jVersion
val logBackClassic = "ch.qos.logback" % "logback-classic" % logBackVersion
val scalaLogging   = "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion
val loggingStack   = Seq(slf4jApi, logBackClassic, scalaLogging)

val typesafeConfig = "com.typesafe" % "config" % "1.3.1"

val circeCore    = "io.circe" %% "circe-core" % circeVersion
val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
val circeJawn    = "io.circe" %% "circe-jawn" % circeVersion
val circe        = Seq(circeCore, circeGeneric, circeJawn)

val javaxMailSun = "com.sun.mail" % "javax.mail" % "1.5.5"

val slick       = "com.typesafe.slick" %% "slick" % slickVersion
val slickHikari = "com.typesafe.slick" %% "slick-hikaricp" % slickVersion
val h2          = "com.h2database" % "h2" % "1.3.176" //watch out! 1.4.190 is beta
val postgres    = "org.postgresql" % "postgresql" % "9.4.1208"
val flyway      = "org.flywaydb" % "flyway-core" % "4.2.0"
val slickStack  = Seq(slick, h2, postgres, slickHikari, flyway)

val scalatest        = "org.scalatest" %% "scalatest" % "3.0.1" % "test"
val unitTestingStack = Seq(scalatest)

val seleniumJava    = "org.seleniumhq.selenium" % "selenium-java" % seleniumVersion % "test"
val seleniumFirefox = "org.seleniumhq.selenium" % "selenium-firefox-driver" % seleniumVersion % "test"
val seleniumStack   = Seq(seleniumJava, seleniumFirefox)

val akkaHttpCore         = "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion
val akkaHttpExperimental = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
val akkaHttpTestkit      = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test"
val akkaHttpSession      = "com.softwaremill.akka-http-session" %% "core" % "0.5.0"
val jwtSession           = "com.softwaremill.akka-http-session" %% "jwt"  % "0.5.1"
val akkaStack            = Seq(akkaHttpCore, akkaHttpExperimental, akkaHttpTestkit, akkaHttpSession)
val swagger              = "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.9.1"
val sprayJson            = "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.10"

// TODO remove this dep
val playWS               = "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.1"
val playStack            = Seq(playWS)

val commonDependencies = unitTestingStack ++ loggingStack

resolvers += "Flyway" at "https://flywaydb.org/repo"

lazy val updateNpm = taskKey[Unit]("Update npm")
lazy val npmTask   = inputKey[Unit]("Run npm with arguments")

lazy val commonSettings = Seq(
  organization := "com.softwaremill",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.12.1",
  crossScalaVersions := Seq(scalaVersion.value, "2.11.8"),
  crossVersion := CrossVersion.binary,
  scalacOptions ++= Seq("-unchecked", "-deprecation"),
  libraryDependencies ++= commonDependencies,
  updateNpm := {
    println("Updating npm dependencies")
    haltOnCmdResultError(Process("npm install", baseDirectory.value / ".." / "ui") !)
  },
  npmTask := {
    val taskName = spaceDelimited("<arg>").parsed.mkString(" ")
    updateNpm.value
    val localNpmCommand = "npm " + taskName
    def buildWebpack() =
      Process(localNpmCommand, baseDirectory.value / ".." / "ui").!
    println("Building with Webpack : " + taskName)
    haltOnCmdResultError(buildWebpack())
  }
)

def haltOnCmdResultError(result: Int) {
  if (result != 0) {
    throw new Exception("Build failed.")
  }
}

lazy val rootProject = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "bootzooka",
    herokuFatJar in Compile := Some((assemblyOutputPath in backend in assembly).value),
    deployHeroku in Compile := ((deployHeroku in Compile) dependsOn (assembly in backend)).value
  )
  .aggregate(backend, ui)


/****************************************************************
  * The backend project uses pillar and cassandra as the data store.
  * Use the pillar plugin to migrate.
  *
  * createKeyspace
  * dropKeyspace
  * migrate
  * cleanMigrate
  ***************************************************************/
lazy val backend: Project = (project in file("backend"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings)
  .settings(Revolver.settings)
  .settings(
    libraryDependencies ++= slickStack ++ akkaStack ++ circe ++ playStack ++
      Seq(javaxMailSun, typesafeConfig, swagger, sprayJson),
    buildInfoPackage := "com.softwaremill.bootzooka.version",
    buildInfoObject := "BuildInfo",
    buildInfoKeys := Seq[BuildInfoKey](
      BuildInfoKey.action("buildDate")(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())),
      // if the build is done outside of a git repository, we still want it to succeed
      BuildInfoKey.action("buildSha")(Try(Process("git rev-parse HEAD").!!.stripLineEnd).getOrElse("?"))
    ),
    compile in Compile := {
      val compilationResult = (compile in Compile).value
      IO.touch(target.value / "compilationFinished")

      compilationResult
    },
    mainClass in Compile := Some("com.softwaremill.bootzooka.Main"),
    // We need to include the whole webapp, hence replacing the resource directory
    unmanagedResourceDirectories in Compile := {
      (unmanagedResourceDirectories in Compile).value ++ List(
        baseDirectory.value.getParentFile / ui.base.getName / "dist"
      )
    },
    assemblyJarName in assembly := "bootzooka.jar",
    assembly := assembly.dependsOn(npmTask.toTask(" run build")).value
  )

lazy val ui = (project in file("ui"))
  .settings(commonSettings: _*)
  .settings(test in Test := (test in Test).dependsOn(npmTask.toTask(" run test")).value)

lazy val uiTests = (project in file("ui-tests"))
  .settings(commonSettings: _*)
  .settings(
    parallelExecution := false,
    libraryDependencies ++= seleniumStack,
    test in Test := (test in Test).dependsOn(npmTask.toTask(" run build")).value
  ) dependsOn backend

RenameProject.settings

fork := true

