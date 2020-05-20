package info.quiquedev.userservice

import doobie.util.transactor.Transactor
import org.flywaydb.core.Flyway
import scala.jdk.CollectionConverters._
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import cats.effect.Async
import cats.effect.ContextShift
import cats.effect.Sync
import pureconfig._
import pureconfig.generic.auto._
import cats.implicits._

object DatabaseUtils {
  def migrateDbAndGetTransactor[F[_]: ContextShift]()(
      implicit A: Async[F]
  ): F[Transactor[F]] =
    for {
      databaseConfig <- getDatabaseConfig
      _ <- migrateDb(databaseConfig)
      xa <- getTransactor(databaseConfig)
    } yield xa

  private def getDatabaseConfig[F[_]](implicit S: Sync[F]): F[DatabaseConfig] =
    ConfigSource.default.load[Config].map(_.db) match {
      case Left(errors) => {
        val errorMsg = errors.toList.map(_.description).mkString("|")
        S.raiseError(ConfigurationLoadError(errorMsg))
      }
      case Right(config) => config.pure[F]
    }

  private def migrateDb[F[_]](
      config: DatabaseConfig
  )(implicit A: Async[F]): F[Unit] =
    Try(
      Flyway.configure
        .dataSource(
          config.jdbcUrl,
          config.credentials.flyway.user,
          config.credentials.flyway.password
        )
        .placeholders(
          Map(
            "dbAppUser" -> config.credentials.app.user,
            "dbAppPassword" -> config.credentials.app.password
          ).asJava
        )
        .load()
        .migrate()
    ) match {
      case Success(_) => A.unit
      case Failure(e) => A.raiseError(e)
    }

  private def getTransactor[F[_]: ContextShift](config: DatabaseConfig)(
      implicit A: Async[F]
  ): F[Transactor[F]] =
    Try(
      Transactor.fromDriverManager[F](
        classOf[org.postgresql.Driver].getName,
        config.jdbcUrl,
        config.credentials.app.user,
        config.credentials.app.password
      )
    ) match {
      case Success(xa) => A.pure(xa)
      case Failure(e)  => A.raiseError(e)
    }

  private final case class Credential(user: String, password: String)
  private final case class Credentials(flyway: Credential, app: Credential)
  private final case class DatabaseConfig(
      jdbcUrl: String,
      credentials: Credentials
  )
  private final case class Config(db: DatabaseConfig)

  final case class ConfigurationLoadError(message: String) extends RuntimeException(message)
}
