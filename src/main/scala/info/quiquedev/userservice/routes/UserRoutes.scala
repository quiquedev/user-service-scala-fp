package info.quiquedev.userservice.routes

import cats.effect.Sync
import cats.implicits._
import info.quiquedev.userservice.routes.dtos.{UserDto, FirstNameDto, LastNameDto, NewUserDto, NewUserDtoValidationError, QueryParamValidationError, SearchLimitDto, UsersDto}
import info.quiquedev.userservice.usecases.UserUsecases
import info.quiquedev.userservice.usecases.domain.{UserId, UserNotFoundError}
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{OptionalQueryParamDecoderMatcher, QueryParamDecoderMatcher}
import org.http4s.{HttpRoutes, _}

object UserRoutes {
  import Codec._

  def value[F[_]: Sync: UserUsecases]: HttpRoutes[F] = {
    val U = UserUsecases[F]
    import U._

    val dsl = new Http4sDsl[F] {}
    import dsl._
import UserDto._

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
          ) :? SearchLimitDtoOptionalParamMatcher(searchLimitDto) =>
        (for {
          searchLimit <- SearchLimitDto.toDomainF(searchLimitDto)
          firstName <- FirstNameDto.toDomainF(firstNameDto)
          lastName <- LastNameDto.toDomainF(lastNameDto)
          users <- findUsersByName(firstName, lastName, searchLimit)
          response <- Ok(UsersDto(users.map(_.toDto)))
        } yield response).recoverWith {
          case QueryParamValidationError(errors) =>
            BadRequest(errors.toList.mkString("|"))
        }
      case GET -> Root / "users" / IntVar(userId) =>
        for {
          maybeUser <- findUserById(UserId(userId))
          response <- maybeUser.map(u => Ok(u.toDto)).getOrElse(NotFound())
        } yield response

      case DELETE -> Root / "users" / IntVar(userId) =>
        (for {
          _ <- deleteUserById(UserId(userId))
          response <- Ok()
        } yield response).recoverWith {
          case UserNotFoundError => NotFound()
        }
    }
  }
}

private object Codec {
  implicit def userDtoEntityEncoder[F[_]: Sync]: EntityEncoder[F, UserDto] =
    jsonEncoderOf

  implicit def userDtoListEntityEncoder[F[_]: Sync]
      : EntityEncoder[F, List[UserDto]] = jsonEncoderOf

  implicit def usersDtoEntityEncoder[F[_]: Sync]: EntityEncoder[F, UsersDto] =
    jsonEncoderOf

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
