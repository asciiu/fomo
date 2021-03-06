import java.text.SimpleDateFormat
import java.util.Date

import sbt._
import Keys._

import scala.util.Try
import scala.sys.process.Process
import complete.DefaultParsers._

val scalaVer            = "2.12.4"
val slf4jVersion        = "1.7.21"
val logBackVersion      = "1.1.7"
val scalaLoggingVersion = "3.5.0"
val slickVersion        = "3.2.1"
val seleniumVersion     = "2.53.0"
val circeVersion        = "0.8.0"
val akkaVersion         = "2.5.6"
val akkaHttpVersion     = "10.0.10"

val slf4jApi       = "org.slf4j" % "slf4j-api" % slf4jVersion
val logBackClassic = "ch.qos.logback" % "logback-classic" % logBackVersion
val scalaLogging   = "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion
val loggingStack   = Seq(slf4jApi, logBackClassic, scalaLogging)

val typesafeConfig = "com.typesafe" % "config" % "1.3.1"

val circeCore    = "io.circe" %% "circe-core" % circeVersion
val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
val circeJawn    = "io.circe" %% "circe-jawn" % circeVersion
val circeParse   = "io.circe" %% "circe-parser" % circeVersion
val circe        = Seq(circeCore, circeGeneric, circeJawn, circeParse)

val javaxMailSun = "com.sun.mail" % "javax.mail" % "1.5.5"

// com.flowy.fomoApi.database
val slick       = "com.typesafe.slick" %% "slick" % slickVersion
val slickHikari = "com.typesafe.slick" %% "slick-hikaricp" % slickVersion
val slickpg     = "com.github.tminglei" %% "slick-pg" % "0.15.4"
val slickpgc    = "com.github.tminglei" %% "slick-pg_circe-json" % "0.15.4"
val slickpgd    = "com.github.tminglei" %% "slick-pg_joda-time" % "0.15.4"
val slickspray  = "com.github.tminglei" %% "slick-pg_spray-json" % "0.15.4"
val slickgen    = "com.typesafe.slick" %% "slick-codegen" % slickVersion
val h2          = "com.h2database" % "h2" % "1.4.192" //watch out! 1.4.190 is beta
val postgres    = "org.postgresql" % "postgresql" % "42.1.4"
val flyway      = "org.flywaydb" % "flyway-core" % "5.0.7"
val slickStack  = Seq(slick, h2, postgres, slickHikari, flyway, slickpg, slickpgc, slickpgd, slickgen, slickspray)
val redisScala  = "com.github.etaty" %% "rediscala" % "1.8.0"

val scalatest        = "org.scalatest" %% "scalatest" % "3.0.1" % "test"
val unitTestingStack = Seq(scalatest)

val seleniumJava    = "org.seleniumhq.selenium" % "selenium-java" % seleniumVersion % "test"
val seleniumFirefox = "org.seleniumhq.selenium" % "selenium-firefox-driver" % seleniumVersion % "test"
val seleniumStack   = Seq(seleniumJava, seleniumFirefox)


// akka toolkits
val akkaActor            = "com.typesafe.akka" %% "akka-actor" % akkaVersion
val akkaClustering       = "com.typesafe.akka" %% "akka-cluster" % akkaVersion
val akkRemote            = "com.typesafe.akka" %% "akka-remote" % akkaVersion
val akkaClusterTools     = "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion
val akkaMultNode         = "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion
val akkaStream           = "com.typesafe.akka" %% "akka-stream" % akkaVersion

val akkaHttp             = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
val akkaHttpTestkit      = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test"
val akkaHttpSession      = "com.softwaremill.akka-http-session" %% "core" % "0.5.0"
val jwtSession           = "com.softwaremill.akka-http-session" %% "jwt"  % "0.5.1"

val akkaStack            = Seq(akkaHttp, akkaHttpTestkit, akkaHttpSession)

val swagger              = "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.11.0"
val sprayJson            = "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
val sigarLoders          = "io.kamon" % "sigar-loader" % "1.6.6-rev002"

val akkaClusterStack     = Seq(akkaActor, akkaClustering, akkRemote, akkaStream, akkaClusterTools, akkaMultNode, scalatest)
val mobilePush           = "com.malliina" %% "mobile-push" % "1.8.0"
val apln                 = "org.mortbay.jetty.alpn" % "alpn-boot" % "8.1.11.v20170118" % Test

// scalatrex
val ws = "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.3"

// string condition interpreters
val scalaCompiler        = "org.scala-lang" % "scala-compiler" % scalaVer
val scalaReflect         = "org.scala-lang" % "scala-reflect" % scalaVer



val commonDependencies = unitTestingStack ++ loggingStack

resolvers ++= Seq(
  "Flyway" at "https://flywaydb.org/repo",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")

lazy val updateNpm = taskKey[Unit]("Update npm")
lazy val npmTask   = inputKey[Unit]("Run npm with arguments")

lazy val commonSettings = Seq(
  fork := true,
  organization := "com.flowy",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := scalaVer,
  crossScalaVersions := Seq(scalaVersion.value, "2.11.8"),
  crossVersion := CrossVersion.binary,
  scalacOptions ++= Seq("-unchecked", "-deprecation"),
  libraryDependencies ++= commonDependencies,
  javaOptions in Test += "-Dconfig.file=src/test/resources/test.conf",
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case PathList("application.conf") => MergeStrategy.first
    case PathList("logback.xml") => MergeStrategy.first
    case PathList("Jenkinsfile") => MergeStrategy.discard
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
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
    name := "fomo",
    herokuFatJar in Compile := Some((assemblyOutputPath in api in assembly).value),
    deployHeroku in Compile := ((deployHeroku in Compile) dependsOn (assembly in api)).value
  )
  .aggregate(api, ui)


/****************************************************************
  * this is the api
  ***************************************************************/
lazy val api: Project = (project in file("api"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings)
  .settings(Revolver.settings)
  .settings(
    assemblyJarName in assembly := "api.jar",
    libraryDependencies ++= slickStack ++ akkaStack ++ akkaClusterStack ++ circe ++
      Seq(javaxMailSun, typesafeConfig, scalaCompiler, scalaReflect, swagger, sprayJson, ws, redisScala),
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
    mainClass in Compile := Some("com.flowy.fomoapi.Main"),
    test in assembly := {},
    // We need to include the whole webapp, hence replacing the resource directory
    unmanagedResourceDirectories in Compile := {
      (unmanagedResourceDirectories in Compile).value ++ List(
        baseDirectory.value.getParentFile / ui.base.getName / "dist"
      )
    }
  )
  .dependsOn(common, bexchange, cache, trailstop)


/****************************************************************
  * all common code shared amongst the projects go here
  ***************************************************************/
lazy val common = (project in file("common"))
  .settings(commonSettings: _*)
  .settings(
    assemblyJarName in assembly := "common.jar",
    name := "common",
    libraryDependencies ++= akkaStack ++ circe ++ slickStack ++ Seq(scalaCompiler, sprayJson, redisScala, scalatest, ws)
  )


lazy val bexchange: Project = (project in file("bittrex-exchange"))
  .settings(commonSettings: _*)
  .settings(
    assemblyJarName in assembly := "bexchange.jar",
    name := "bittrex-exchange",
    libraryDependencies ++= akkaClusterStack ++ Seq(sprayJson),
    javaOptions in reStart ++= addAlpnPath((managedClasspath in Runtime).value)
  )
  .dependsOn(cache, common, trailstop, bfeed, notification)


/****************************************************************
  * this is the bittrex signalr client
  ***************************************************************/
lazy val bfeed: Project = (project in file("bittrex-websocket"))
  .settings(commonSettings: _*)
  .settings(
    assemblyJarName in assembly := "bfeed.jar",
    name := "bittrex-websocket",
    libraryDependencies ++= akkaClusterStack ++ Seq(sprayJson)
  )
  .dependsOn(common)


/****************************************************************
  * this is the bittrex signalr client
  ***************************************************************/
lazy val binance_websocket: Project = (project in file("binance-websocket"))
  .settings(commonSettings: _*)
  .settings(
    assemblyJarName in assembly := "binance-websocket.jar",
    name := "binance-websocket",
    libraryDependencies ++= akkaClusterStack ++ Seq(sprayJson)
  )
  .dependsOn(common)


lazy val cache: Project = (project in file("cache-service"))
  .settings(commonSettings: _*)
  .settings(
    assemblyJarName in assembly := "cache.jar",
    name := "cache-service",
    libraryDependencies ++= akkaClusterStack
  )
  .dependsOn(common)

/****************************************************************
  * trailing stop
  ***************************************************************/
lazy val trailstop: Project = (project in file("trailing-stop"))
  .settings(commonSettings: _*)
  .settings(
    assemblyJarName in assembly := "trailstop.jar",
    name := "trailing-stop",
    libraryDependencies ++= akkaClusterStack
  )
  .dependsOn(common)

/****************************************************************
  * notification
  ***************************************************************/
lazy val notification: Project = (project in file("notification"))
  .settings(commonSettings: _*)
  .settings(
    assemblyJarName in assembly := "notification.jar",
    name := "notification",
    libraryDependencies ++= akkaClusterStack ++ Seq(mobilePush, apln),
    // Adds ALPN to the boot classpath for Http2 support
    javaOptions in reStart ++= addAlpnPath((managedClasspath in Runtime).value)
  )
  .dependsOn(common)


lazy val ui = (project in file("ui"))
  .settings(commonSettings: _*)
  .settings(test in Test := (test in Test).dependsOn(npmTask.toTask(" run test")).value)


lazy val uiTests = (project in file("ui-tests"))
  .settings(commonSettings: _*)
  .settings(
    parallelExecution := false,
    libraryDependencies ++= seleniumStack,
    test in Test := (test in Test).dependsOn(npmTask.toTask(" run build")).value
  ) dependsOn api


def addAlpnPath(attList : Keys.Classpath): Seq[String] = {
  for {
    file <- attList.map(_.data)
    path = file.getAbsolutePath if path.contains("alpn-boot")
  } yield { println(s"ALPN path: $path"); "-Xbootclasspath/p:" + path}
}

RenameProject.settings
fork := true

