package com.flowy.common.database.postgres.schema

import java.time.OffsetDateTime
import java.util.UUID

import com.flowy.common.models._
import com.flowy.common.utils.sql.SqlDatabase

trait SqlUserDeviceSchema {
  protected val database: SqlDatabase

  import database._
  import database.driver.api._

  protected val userDevices = TableQuery[UserDevices]

  implicit val typeMapper = MappedColumnType.base[DeviceType.Value, String](
    { os => os.toString }, { str => DeviceType.withName(str) }
  )

  protected class UserDevices(tag: Tag) extends Table[UserDevice](tag, "user_devices") {
    // format: OFF
    def id              = column[UUID]("id", O.PrimaryKey)
    def userId          = column[UUID]("user_id")
    def deviceType      = column[String]("device_type")
    def deviceId        = column[String]("device_id")
    def deviceTokie     = column[String]("device_token")
    def createdOn       = column[OffsetDateTime]("created_on")
    def updatedOn       = column[OffsetDateTime]("updated_on")

    def * = (id, userId, deviceType, deviceId, deviceTokie) <>
      ((UserDevice.applyWithId _).tupled, UserDevice.unapply)

    // format: ON
  }
}
