package database.cassandra

import java.util.UUID

import com.softwaremill.bootzooka.user.UserId
import database.{AppDatabase, UserDao}
import models.{BasicUserData, User}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CassandraUserDao extends UserDao {

  val userIdDao = AppDatabase.UsersByIdDao
  val userEmailDao = AppDatabase.UsersByEmailDao

  def add(user: User): Future[Unit] = {
    userIdDao.add(user)
    userEmailDao.add(user)
    Future.successful((): Unit)
  }

  def findById(userId: UUID): Future[Option[User]] =  userIdDao.findById(userId)

  def findBasicDataById(userId: UUID): Future[Option[BasicUserData]] = userIdDao.findBasicDataById(userId)

  def findByEmail(email: String): Future[Option[User]] = userEmailDao.findByEmail(email)

  def findByLowerCasedLogin(login: String): Future[Option[User]] = ???

  def findByLoginOrEmail(loginOrEmail: String): Future[Option[User]] = ???

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

  def changeLogin(userId: UserId, newLogin: String): Future[Unit] = ???

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
