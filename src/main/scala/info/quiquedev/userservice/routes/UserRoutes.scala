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
import info.quiquedev.userservice.usecases.model.{TooManyMailsError, _}
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{
  OptionalQueryParamDecoderMatcher,
  QueryParamDecoderMatcher
}
import org.http4s.{HttpRoutes, _}
import info.quiquedev.userservice.Logger

object UserRoutes {
  import Codec._

  def value[F[_]: Sync: UserUsecases]: HttpRoutes[F] = {
    val U = UserUsecases[F]
    implicit val L = Logger.impl[F]("UserRoutes")
    implicit val dsl = new Http4sDsl[F] {}

    import U._
    import UserDto._
    import dsl._

    HttpRoutes.of[F] {
      case req @ POST -> Root / "users" =>
        (for {
          newUserDto <- req.as[NewUserDto]
          newUser <- newUserDto.toModelF
          createdUser <- createUser(newUser)
          response <- Created(createdUser.toDto)
        } yield response).recoverWith {
          case RequestBodyValidationError(errors) =>
            BadRequest(errors.toList.mkString(","))
        }.andHandleOtherErrors
      case GET -> Root / "users" :? FirstNameMatcher(firstNameDto) :? LastNameMatcher(
            lastNameDto
          ) :? SearchLimitMatcher(searchLimitDto) =>
        (for {
          searchLimit <- SearchLimitDto.toModelF(searchLimitDto)
          firstName <- FirstNameDto.toModelF(firstNameDto)
          lastName <- LastNameDto.toModelF(lastNameDto)
          users <- findUsersByName(firstName, lastName, searchLimit)
          response <- Ok(UsersDto(users.map(_.toDto)))
        } yield response).recoverWith {
          case QueryParamValidationError(errors) =>
            BadRequest(errors.toList.mkString("|"))
        }.andHandleOtherErrors
      case GET -> Root / "users" / IntVar(userId) =>
        (for {
          maybeUser <- findUserById(UserId(userId))
          response <- maybeUser.map(u => Ok(u.toDto)).getOrElse(NotFound())
        } yield response).andHandleOtherErrors

      case DELETE -> Root / "users" / IntVar(userId) =>
        (for {
          _ <- deleteUserById(UserId(userId))
          response <- Ok()
        } yield response).recoverWith {
          case UserNotFoundError => NotFound()
        }.andHandleOtherErrors
      case req @ POST -> Root / "users" / IntVar(userId) / "mails" =>
        (for {
          newMailDto <- req.as[NewMailDto]
          mail <- newMailDto.toModelF
          updatedUser <- addMailToUser(UserId(userId), mail)
          response <- Created(updatedUser.toDto)
        } yield response).recoverWith {
          case RequestBodyValidationError(errors) =>
            BadRequest(errors.toList.mkString("|"))
          case UserNotFoundError => NotFound()
          case TooManyMailsError => Conflict()
        }.andHandleOtherErrors
      case req @ PUT -> Root / "users" / IntVar(userId) / "mails" / IntVar(
            mailId
          ) =>
        (for {
          newMailDto <- req.as[NewMailDto]
          mail <- newMailDto.toModelF
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
        }.andHandleOtherErrors
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
        }.andHandleOtherErrors
      case req @ POST -> Root / "users" / IntVar(userId) / "numbers" =>
        (for {
          newNumberDto <- req.as[NewNumberDto]
          number <- newNumberDto.toModelF
          updatedUser <- addNumberToUser(UserId(userId), number)
          response <- Created(updatedUser.toDto)
        } yield response).recoverWith {
          case RequestBodyValidationError(errors) =>
            BadRequest(errors.toList.mkString("|"))
          case UserNotFoundError   => NotFound()
          case TooManyNumbersError => Conflict()
        }.andHandleOtherErrors
      case req @ PUT -> Root / "users" / IntVar(userId) / "numbers" / IntVar(
            numberId
          ) =>
        (for {
          newNumberDto <- req.as[NewNumberDto]
          number <- newNumberDto.toModelF
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
        }.andHandleOtherErrors
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
        }.andHandleOtherErrors
    }
  }

  private implicit final class ResponseExtensions[F[_]](
      val value: F[Response[F]]
  ) extends AnyVal {
    def andHandleOtherErrors(
        implicit D: Http4sDsl[F],
        L: Logger[F],
        S: Sync[F]
    ): F[Response[F]] = {
      import D._

      value.recoverWith {
        case unhandledError =>
          L.error("unhandled error", unhandledError) *> InternalServerError(
            "something bad happened! please try later or contact our team"
          )
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

  object FirstNameMatcher
      extends QueryParamDecoderMatcher[FirstNameDto]("firstName")

  implicit val lastNameDtoQueryParamDecoder: QueryParamDecoder[LastNameDto] =
    QueryParamDecoder[String].map(LastNameDto.apply)

  object LastNameMatcher
      extends QueryParamDecoderMatcher[LastNameDto]("lastName")

  implicit val searchLimitDtoOptionalQueryParamDecoder
      : QueryParamDecoder[SearchLimitDto] =
    QueryParamDecoder[Int].map(SearchLimitDto.apply)

  object SearchLimitMatcher
      extends OptionalQueryParamDecoderMatcher[SearchLimitDto]("searchLimit")

}
