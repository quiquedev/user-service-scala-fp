package info.quiquedev.userservice

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s._
import org.http4s.circe._
import java.time.Instant
import info.quiquedev.userservice.UserUsecases
import Dto.UserDto._
import Dto.NewUserDto
import Dto.NewUserDto._
import info.quiquedev.userservice.Dto.NewUserDtoValidationError

object Routes {
  private def health[F[_]: Sync]: HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "health" => Ok()
    }
  }

  private def users[F[_]: Sync: UserUsecases]: HttpRoutes[F] = {
    val U = UserUsecases[F]
    import U._

    val dsl = new Http4sDsl[F] {}
    import Domain._

    import dsl._

    HttpRoutes.of[F] {
      case req @ POST -> Root / "users" =>
        (for {
          newUserDto <- req.as[NewUserDto]
          newUser <- newUserDto.toDomainF
          createdUser <- createUser(newUser)
          response <- Created(createdUser.toDto)
        } yield response).recoverWith {
          case NewUserDtoValidationError(errors) => {
val e = errors.toList.mkString(",")
            BadRequest(errors.toList.mkString(","))}
        }
    }
  }

  def all[F[_]: Sync: UserUsecases]: HttpRoutes[F] = health <+> users

}
