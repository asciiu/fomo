package database.cassandra

import database.CassandraDatabase
import database.dao.RememberMeTokenDao
import models.RememberMeToken

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CassandraRememberMeTokenDao(val database: CassandraDatabase) extends RememberMeTokenDao {
  val dao = database.RememberTokenDao

  def findBySelector(selector: String): Future[Option[RememberMeToken]] = dao.findBySelector(selector)
  def add(data: RememberMeToken): Future[Unit] = dao.add(data).map(_ => (): Unit)
  def remove(selector: String): Future[Unit] = dao.remove(selector).map(_ => (): Unit)
}
