package info.quiquedev.userservice

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import scala.concurrent.ExecutionContext.global
import java.time.Clock
import cats.Monad
import cats.implicits._

object UserServiceServer {
  def stream[F[_]: ConcurrentEffect: Timer: ContextShift: Monad](
      implicit
      clock: Clock
  ): Stream[F, Nothing] = {

    for {
      transactor <- Stream.eval(DatabaseUtils.migrateDbAndGetTransactor())
      _ <- BlazeClientBuilder[F](global).stream

      httpApp = {
        implicit val U: UserUsecases[F] = ???

        Routes.all[F].orNotFound
      }

      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
  }.drain
}
