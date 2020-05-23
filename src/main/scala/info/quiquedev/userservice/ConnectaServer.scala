package info.quiquedev.userservice


import cats.Monad
import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import doobie.util.transactor.Transactor
import fs2.Stream
import info.quiquedev.userservice.routes.UserRoutes
import info.quiquedev.userservice.usecases.UserUsecases
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

import scala.concurrent.ExecutionContext.global

object ConnectaServer {
  def stream[F[_]: ConcurrentEffect: Timer: ContextShift: Monad]
      : Stream[F, Nothing] = {

    for {
      transactor <- Stream.eval(DatabaseUtils.migrateDbAndGetTransactor())
      _ <- BlazeClientBuilder[F](global).stream

      httpApp = {
        implicit val xa: Transactor[F] = transactor
        implicit val U: UserUsecases[F] = UserUsecases.impl[F]

        UserRoutes.value[F].orNotFound
      }

      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
  }.drain
}
