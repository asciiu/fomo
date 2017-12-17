package com.flowy.fomoapi.slick

import java.io.File

import com.typesafe.config.ConfigFactory
import slick.sql.SqlProfile.ColumnOption
import Config._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  *  This customizes the Slick code generator. We only do simple name mappings.
  *  For a more advanced example see https://github.com/cvogt/slick-presentation/tree/scala-exchange-2013
  */
object CustomizedCodeGenerator {
  import scala.concurrent.ExecutionContext.Implicits.global

  def main(args: Array[String]): Unit = {
    // write the generated results to file
    val future = codegen.map(
      _.writeToFile(
        driver,
        "backend/src/main/scala",
        "slick",
        "Tables",
        "Tables.scala"
      )
    )

    Await.result(
      future,
      20.seconds
    )
  }

  val config = ConfigFactory.parseFile(new File("backend/src/main/resources/application.conf"))

  val url = config.getString("bootzooka.db.postgres.properties.url")
  val user = config.getString("bootzooka.db.postgres.properties.user")
  val pass = config.getString("bootzooka.db.postgres.properties.password")
  val driver = "slick.MyPostgresDriver"

  val slickDriver = {
    val value = driver

    //remove last $ character
    val pos = value.length - 1
    if (value.charAt(pos) == '$') {
      value.substring(0, pos)
    } else {
      value
    }
  }

  val db = slickProfile.api.Database.forURL(url = url, user = user, password = pass, driver = driver)

  val ignore = Seq("schema_version")

  lazy val codegen = db.run {
    slickProfile.defaultTables.map(_.filter ( t => !ignore.contains(t.name.name)))
      .flatMap( slickProfile.createModelBuilder(_, false).buildModel )
  }.map { model =>
    new slick.codegen.SourceCodeGenerator(model) {
      override def tableName = dbTableName => dbTableName match {
        case "orders" => "Order"
        case _ => super.tableName(dbTableName)
      }
      override def Table = new Table(_) { table =>
        override def Column = new Column(_) { column =>
          // customize db type -> scala type mapping, pls adjust it according to your environment
          override def rawType: String = model.tpe match {
            case "java.sql.Date" => "java.time.LocalDate"
            case "java.sql.Time" => "java.time.LocalTime"
            case "java.sql.Timestamp" => "java.time.OffsetDateTime"
            // currently, all types that's not built-in support were mapped to `String`
            case "String" => model.options.find(_.isInstanceOf[ColumnOption.SqlType]).map(_.asInstanceOf[ColumnOption.SqlType].typeName).map({
              case "hstore" => "Map[String, String]"
              case "geometry" => "com.vividsolutions.jts.geom.Geometry"
              case "int8[]" => "List[Long]"
              case _ =>  "String"
            }).getOrElse("String")
            case _ =>
              super.rawType
          }
        }
      }

      // ensure to use our customized postgres driver at `import profile.simple._`
      override def packageCode(profile: String, pkg: String, container: String, parentType: Option[String]) : String = {
        s"""
package ${pkg}
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object ${container} extends {
  val profile = ${profile}
} with ${container}
/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait ${container}${parentType.map(t => s" extends $t").getOrElse("")} {
  val profile: $profile
  import profile.api._
  ${indent(code)}
}
      """.trim()
      }
    }
  }
}

