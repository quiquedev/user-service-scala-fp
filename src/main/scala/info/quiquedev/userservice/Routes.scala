package info.quiquedev.userservice

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s._
import org.http4s.circe._
import java.time.Instant
import info.quiquedev.userservice.UserUsecases
import Dto._
import Dto.UserDto._
import Dto.NewUserDto
import Dto.NewUserDto._
import info.quiquedev.userservice.Dto.NewUserDtoValidationError
import Dto.FirstNameDto
import Dto.LastNameDto
import io.circe.generic.auto._
import Dto.SearchLimitDto._
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher

object Routes {
  import Codec._

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
          case NewUserDtoValidationError(errors) =>
            BadRequest(errors.toList.mkString(","))
        }
      case GET -> Root / "users" :? FirstNameDtoParamMatcher(firstNameDto) :? LastNameDtoParamMatcher(
            lastNameDto
          ) :? SearchLimitDtoOptionalParamMatcher(searchLimitDto) => {
        for {
          searchLimit <- SearchLimitDto.toDomainF(searchLimitDto)
          firstName <- FirstNameDto.toDomainF(firstNameDto)
          lastName <- LastNameDto.toDomainF(lastNameDto)
          users <- findUserByName(firstName, lastName, searchLimit)
          response <- Ok(users.map(_.toDto))
        } yield response
      }
    }
  }

  def all[F[_]: Sync: UserUsecases]: HttpRoutes[F] = health <+> users
}

private object Codec {
  implicit def userDtoEntityEncoder[F[_]: Sync]: EntityEncoder[F, UserDto] =
    jsonEncoderOf

  implicit def userDtoListEntityEncoder[F[_]: Sync]
      : EntityEncoder[F, List[UserDto]] = jsonEncoderOf

  implicit def newUserDtoEntityDecoder[F[_]: Sync]
      : EntityDecoder[F, NewUserDto] = jsonOf

  implicit val firstNameDtoQueryParamDecoder: QueryParamDecoder[FirstNameDto] =
    QueryParamDecoder[String].map(FirstNameDto.apply)

  object FirstNameDtoParamMatcher
      extends QueryParamDecoderMatcher[FirstNameDto]("firstName")

  implicit val lastNameDtoQueryParamDecoder: QueryParamDecoder[LastNameDto] =
    QueryParamDecoder[String].map(LastNameDto.apply)

  object LastNameDtoParamMatcher
      extends QueryParamDecoderMatcher[LastNameDto]("lastName")

  implicit val searchLimitDtoOptionalQueryParamDecoder
      : QueryParamDecoder[SearchLimitDto] =
    QueryParamDecoder[Int].map(SearchLimitDto.apply)

  object SearchLimitDtoOptionalParamMatcher
      extends OptionalQueryParamDecoderMatcher[SearchLimitDto]("searchLimit")

}
