package info.quiquedev.userservice.routes

import cats.effect.Sync
import cats.implicits._
import info.quiquedev.userservice.routes.dtos.{
  FirstNameDto,
  LastNameDto,
  NewMailDto,
  NewNumberDto,
  NewUserDto,
  QueryParamValidationError,
  RequestBodyValidationError,
  SearchLimitDto,
  UserDto,
  UsersDto
}
import info.quiquedev.userservice.usecases.UserUsecases
import info.quiquedev.userservice.usecases.domain.{TooManyMailsError, _}
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{
  OptionalQueryParamDecoderMatcher,
  QueryParamDecoderMatcher
}
import org.http4s.{HttpRoutes, _}

object UserRoutes {
  import Codec._

  def value[F[_]: Sync: UserUsecases]: HttpRoutes[F] = {
    val U = UserUsecases[F]
    val dsl = new Http4sDsl[F] {}

    import U._
    import UserDto._
    import dsl._

    HttpRoutes.of[F] {
      case req @ POST -> Root / "users" =>
        (for {
          newUserDto <- req.as[NewUserDto]
          newUser <- newUserDto.toDomainF
          createdUser <- createUser(newUser)
          response <- Created(createdUser.toDto)
        } yield response).recoverWith {
          case RequestBodyValidationError(errors) =>
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
      case req @ POST -> Root / "users" / IntVar(userId) / "mails" =>
        (for {
          newMailDto <- req.as[NewMailDto]
          mail <- newMailDto.toDomainF
          updatedUser <- addMailToUser(UserId(userId), mail)
          response <- Created(updatedUser.toDto)
        } yield response).recoverWith {
          case RequestBodyValidationError(errors) =>
            BadRequest(errors.toList.mkString("|"))
          case UserNotFoundError => NotFound()
          case TooManyMailsError => Conflict()
        }
      case req @ PUT -> Root / "users" / IntVar(userId) / "mails" / IntVar(
            mailId
          ) =>
        (for {
          newMailDto <- req.as[NewMailDto]
          mail <- newMailDto.toDomainF
          updatedUser <- updateMailFromUser(
            UserId(userId),
            MailWithId(MailId(mailId), Mail(mail.value))
          )
          response <- Ok(updatedUser.toDto)
        } yield response).recoverWith {
          case RequestBodyValidationError(errors) =>
            BadRequest(errors.toList.mkString("|"))
          case MailNotFoundError => NotFound()
          case UserNotFoundError => Gone()
        }
      case DELETE -> Root / "users" / IntVar(userId) / "mails" / IntVar(
            mailId
          ) =>
        (for {
          updatedUser <- deleteMailFromUser(
            UserId(userId),
            MailId(mailId)
          )
          response <- Ok(updatedUser.toDto)
        } yield response).recoverWith {
          case MailNotFoundError   => NotFound()
          case NotEnoughMailsError => Conflict()
          case UserNotFoundError   => Gone()
        }
      case req @ POST -> Root / "users" / IntVar(userId) / "numbers" =>
        (for {
          newNumberDto <- req.as[NewNumberDto]
          number <- newNumberDto.toDomainF
          updatedUser <- addNumberToUser(UserId(userId), number)
          response <- Created(updatedUser.toDto)
        } yield response).recoverWith {
          case RequestBodyValidationError(errors) =>
            BadRequest(errors.toList.mkString("|"))
          case UserNotFoundError   => NotFound()
          case TooManyNumbersError => Conflict()
        }
      case req @ PUT -> Root / "users" / IntVar(userId) / "numbers" / IntVar(
            numberId
          ) =>
        (for {
          newNumberDto <- req.as[NewNumberDto]
          number <- newNumberDto.toDomainF
          updatedUser <- updateNumberFromUser(
            UserId(userId),
            NumberWithId(NumberId(numberId), Number(number.value))
          )
          response <- Ok(updatedUser.toDto)
        } yield response).recoverWith {
          case RequestBodyValidationError(errors) =>
            BadRequest(errors.toList.mkString("|"))
          case NumberNotFoundError => NotFound()
          case UserNotFoundError   => Gone()
        }
      case DELETE -> Root / "users" / IntVar(userId) / "numbers" / IntVar(
            numberId
          ) =>
        (for {
          updatedUser <- deleteNumberFromUser(
            UserId(userId),
            NumberId(numberId)
          )
          response <- Ok(updatedUser.toDto)
        } yield response).recoverWith {
          case NumberNotFoundError   => NotFound()
          case NotEnoughNumbersError => Conflict()
          case UserNotFoundError     => Gone()
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

  implicit def newMailDtoEntityDecoder[F[_]: Sync]
      : EntityDecoder[F, NewMailDto] = jsonOf

  implicit def newNumberDtoEntityDecoder[F[_]: Sync]
      : EntityDecoder[F, NewNumberDto] = jsonOf

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
