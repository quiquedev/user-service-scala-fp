package info.quiquedev.userservice.usecases

import cats.effect.IO
import doobie.util.transactor.Transactor
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters._

object FlywayMigrator {
  private val dbAppUser = "test_user"
  private val dbAppPassword = "test_password"

  def migrateDbAndGetTransactorIO(
      jdbcUrl: String,
      username: String,
      password: String
  ): Transactor[IO] = {
    Flyway.configure
      .dataSource(
        jdbcUrl,
        username,
        password
      )
      .placeholders(
        Map("dbAppUser" -> dbAppUser, "dbAppPassword" -> dbAppPassword).asJava
      )
      .load()
      .migrate()

    implicit val contextShift = IO.contextShift(global)
    Transactor.fromDriverManager[IO](
      classOf[org.postgresql.Driver].getName,
      jdbcUrl,
      dbAppUser,
      dbAppPassword
    )
  }
}
