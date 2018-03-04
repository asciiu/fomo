package com.flowy.fomoapi.database.dao

import com.softwaremill.bootzooka.user._
import com.flowy.common.models.{UserData, User}

import scala.concurrent.Future

// TODO depcreate this stuff
trait UserDao {
   def add(user: User): Future[Unit]
   def findById(userId: UserId): Future[Option[User]]
   def findBasicDataById(userId: UserId): Future[Option[UserData]]
   def findByEmail(email: String): Future[Option[User]]
   def changePassword(userId: UserId, newPassword: String): Future[Unit]
   def changeEmail(userId: UserId, newEmail: String): Future[Unit]
}
