package com.softwaremill.bootzooka.user.domain

import java.time.OffsetDateTime
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

import com.datastax.driver.core.ConsistencyLevel
import com.outworkers.phantom.builder.query.InsertQuery
import com.softwaremill.bootzooka.common.Utils
import com.softwaremill.bootzooka.user._
import com.outworkers.phantom.dsl._
import com.softwaremill.bootzooka.user.application.UserDao

import scala.concurrent.Future
import scala.util.{Failure, Success}

case class User(
                 id: UUID,
                 email: String,
                 firstName: String,
                 lastName: String,
                 passwordHash: String,
                 salt: String,
                 createdAt: DateTime,
                 updatedAt: DateTime
               )

// This is a phantom cassandra model that maps the table to the User case class
abstract class Users extends Table[Users, User] with UserDao {

  object id extends UUIDColumn with PartitionKey
  object email extends StringColumn with PartitionKey
  object first_name extends StringColumn
  object last_name extends StringColumn
  object password_hash extends StringColumn
  object salt extends StringColumn
  object created_at extends DateTimeColumn
  object updated_at extends DateTimeColumn

  def add(user: User): Future[Unit] = {
    store(user)
      .consistencyLevel_=(ConsistencyLevel.ALL)
      .future()
      .map { _ => Unit}
  }

  /**
    * Note: this was supposed to be generated by Phantom!
    *
    * @param record
    * @return
    */
  def store(record: User): InsertQuery.Default[Users, User] = {
    insert
      .value(_.id, record.id)
      .value(_.email, record.email)
      .value(_.first_name, record.firstName)
      .value(_.last_name, record.lastName)
      .value(_.password_hash, record.passwordHash)
      .value(_.created_at, record.createdAt)
      .value(_.updated_at, record.updatedAt)
  }

  def findById(id: UUID): Future[Option[User]] = {
    select.where(_.id eqs id).one()
  }

  def findBasicDataById(userId: UUID): Future[Option[BasicUserData]] = {
    select.where(_.id eqs userId).one().map{ user =>
      user match {
        case Some(u) =>
          Some(BasicUserData(u.id, u.firstName, u.lastName, u.email, OffsetDateTime.parse(u.createdAt.toString)))
        case None => None
      }
    }
  }

  def findByEmail(email: String): Future[Option[User]] = {
    select.where(_.email eqs email).one()
  }

  def findByLowerCasedLogin(login: String): Future[Option[User]] = ???

  def findByLoginOrEmail(loginOrEmail: String): Future[Option[User]] = ???

  def changePassword(userId: UserId, newPassword: String): Future[Unit] = ???

  def changeLogin(userId: UserId, newLogin: String): Future[Unit] = ???

  def changeEmail(userId: UserId, newEmail: String): Future[Unit] = ???
}


object User {

  def withRandomUUID(
                     email: String,
                     firstName: String,
                     lastName: String,
                     password: String,
                     salt: String,
                     createdAt: DateTime,
                     updatedAt: DateTime) =
    User(UUID.randomUUID(), email, firstName, lastName,
      encryptPassword(password, salt), salt, createdAt, updatedAt)

  def encryptPassword(password: String, salt: String): String = {
    // 10k iterations takes about 10ms to encrypt a password on a 2013 MacBook
    val keySpec          = new PBEKeySpec(password.toCharArray, salt.getBytes, 10000, 128)
    val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val bytes            = secretKeyFactory.generateSecret(keySpec).getEncoded
    Utils.toHex(bytes)
  }

  def passwordsMatch(password: String, user: User): Boolean =
    Utils.constantTimeEquals(
      user.passwordHash,
      encryptPassword(password, user.salt)
    )
}

case class BasicUserData(id: UserId, first: String, last: String, email: String, createdOn: OffsetDateTime)

object BasicUserData {
  def fromUser(user: User) = new BasicUserData(user.id, user.firstName, user.lastName, user.email, OffsetDateTime.parse(user.createdAt.toString))
}
