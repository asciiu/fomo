package database.cassandra

import java.util.UUID

import com.softwaremill.bootzooka.user.UserId
import database.CassandraDatabase
import database.dao.UserDao
import models.{BasicUserData, User}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CassandraUserDao(val database: CassandraDatabase) extends UserDao {

  val userIdDao = database.UsersByIdDao
  val userEmailDao = database.UsersByEmailDao

  def add(user: User): Future[Unit] = {
    userIdDao.add(user)
    userEmailDao.add(user)
    Future.successful((): Unit)
  }

  def findById(userId: UUID): Future[Option[User]] =  userIdDao.findById(userId)

  def findBasicDataById(userId: UUID): Future[Option[BasicUserData]] = userIdDao.findBasicDataById(userId)

  def findByEmail(email: String): Future[Option[User]] = userEmailDao.findByEmail(email)

  def findByLoginOrEmail(loginOrEmail: String): Future[Option[User]] = findByEmail(loginOrEmail)

  def changePassword(userId: UserId, newPassword: String): Future[Unit] = {
    findById(userId).map { userOpt =>
      userOpt match {
        case Some(user) =>
          userIdDao.changePassword(user.id, newPassword)
          userEmailDao.changePassword(user.email, newPassword)
        case None => ???
      }
    }
  }

  def changeEmail(userId: UserId, newEmail: String): Future[Unit] = {
    findById(userId).map { userOpt =>
      userOpt match {
        case Some(user) =>
          userIdDao.changeEmail(userId, newEmail)

          // the email is the primary key so we need to delete it
          // and then add the new email user
          userEmailDao.deleteEmail(user.email)
          userEmailDao.add(user.copy(email = newEmail))
        case None => ???
      }
    }

  }

}
