package services

import java.util.Locale

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import com.softwaremill.bootzooka.ServerConfig
import com.softwaremill.bootzooka.common.sql.{DatabaseConfig, SqlDatabase}
import com.softwaremill.bootzooka.email.application.{DummyEmailService, EmailConfig, EmailTemplatingEngine, SmtpEmailService}
import com.softwaremill.bootzooka.passwordreset.application.PasswordResetConfig
import com.softwaremill.bootzooka.user.application._
import com.softwaremill.session.{SessionConfig, SessionManager}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import database.postgres.{SqlPasswordResetCodeDao, SqlRememberMeTokenDao, SqlUserDao}
import routes.Routes

import scala.concurrent.{ExecutionContext, Future}


trait DependencyWiring extends StrictLogging {
  def system: ActorSystem

  lazy val config = new PasswordResetConfig with EmailConfig with DatabaseConfig with ServerConfig {
    override def rootConfig = ConfigFactory.load()
  }

  implicit val daoExecutionContext = system.dispatchers.lookup("dao-dispatcher")

  //lazy val sqlDatabase = SqlDatabase.createPostgresFromConfig(config)
  lazy val sqlDatabase = SqlDatabase.create(config)
  lazy val userDao = new SqlUserDao(sqlDatabase)(daoExecutionContext)
  lazy val codeDao = new SqlPasswordResetCodeDao(sqlDatabase)(daoExecutionContext)
  lazy val rememberMeTokenDao = new SqlRememberMeTokenDao(sqlDatabase)(daoExecutionContext)

  lazy val serviceExecutionContext = system.dispatchers.lookup("service-dispatcher")

  lazy val emailService = if (config.emailEnabled) {
    new SmtpEmailService(config)(serviceExecutionContext)
  } else {
    logger.info("Starting with fake email sending service. No emails will be sent.")
    new DummyEmailService
  }

  lazy val emailTemplatingEngine = new EmailTemplatingEngine

  lazy val userService = new UserService(
    userDao,
    emailService,
    emailTemplatingEngine
  )(serviceExecutionContext)

  lazy val passwordResetService = new PasswordResetService(
    userDao,
    codeDao,
    emailService,
    emailTemplatingEngine,
    config
  )(serviceExecutionContext)

  lazy val refreshTokenStorage = new RefreshTokenStorageImpl(rememberMeTokenDao, system)(serviceExecutionContext)
}

class HttpService()
                 (implicit executionContext: ExecutionContext,
                  implicit val actorSystem: ActorSystem,
                  implicit val materializer: ActorMaterializer) extends StrictLogging {
  def start(): (Future[ServerBinding], DependencyWiring) = {
    Locale.setDefault(Locale.US) // set default locale to prevent from sending cookie expiration date in polish format
    //import _system.dispatcher
    //import actorSystem.dispatcher

    val modules = new DependencyWiring with Routes {

      lazy val sessionConfig = SessionConfig.fromConfig(config.rootConfig).copy(sessionEncryptData = true)

      implicit lazy val ec                                      = actorSystem.dispatchers.lookup("akka-http-routes-dispatcher")
      implicit lazy val sessionManager: SessionManager[Session] = new SessionManager[Session](sessionConfig)
      implicit lazy val materializer                            = materializer
      lazy val system                                           = actorSystem
    }

    logger.info("Server secret: " + modules.sessionConfig.serverSecret.take(3) + "...")

    modules.sqlDatabase.updateSchema()

    (Http().bindAndHandle(modules.routes, modules.config.serverHost, modules.config.serverPort), modules)
  }
}
