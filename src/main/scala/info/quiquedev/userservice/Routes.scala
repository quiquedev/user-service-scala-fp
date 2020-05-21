package info.quiquedev.userservice

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s._
import org.http4s.circe._
import java.time.Instant
import info.quiquedev.userservice.UserUsecases


object Routes {
  private def health[F[_]: Sync]: HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "health" => Ok()
    }
  }

  private def users[F[_]: Sync: UserUsecases]: HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import Domain._

    import dsl._

    HttpRoutes.of[F] {
      ???
    }
  }

  def all[F[_]: Sync: UserUsecases]: HttpRoutes[F] = health <+> users
  
}
