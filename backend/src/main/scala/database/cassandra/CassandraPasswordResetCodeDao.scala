package database.cassandra

import com.softwaremill.bootzooka.passwordreset.domain.PasswordResetCode
import database.CassandraDatabase
import database.dao.PasswordResetCodeDao

import scala.concurrent.{ExecutionContext, Future}

class CassandraPasswordResetCodeDao(val database: CassandraDatabase)(implicit executionContext: ExecutionContext)
  extends PasswordResetCodeDao {

  private val dao = database.PasswordResetCodeDao

  def add(code: PasswordResetCode): Future[Unit] = {
    dao.add(code).map(_ => (): Unit)
  }

  def findByCode(code: String): Future[Option[PasswordResetCode]] = dao.findByCode(code)

  def remove(code: PasswordResetCode): Future[Unit] = {
    dao.remove(code.code).map(_ => (): Unit)
  }
}
