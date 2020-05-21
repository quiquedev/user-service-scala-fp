package info.quiquedev.userservice

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.data.Validated
import info.quiquedev.userservice._
import cats.implicits._
import doobie.util.Get
import doobie.util.Put
import cats.data.Validated._
import io.circe.generic.extras.encoding.UnwrappedEncoder._
import io.circe.generic.extras.decoding.UnwrappedDecoder._
import io.circe._
import io.circe.generic.auto._
import info.quiquedev.userservice.Domain._

object Dto {
  private type ValidationResult = Validated[NonEmptyList[String], Unit]
  private type ValidationResults = NonEmptyList[ValidationResult]

  private val FirstNameMaxLength = 500
  private val LastNameMaxLength = 500
  private val MailMaxLength = 500
  private val MaxMails = 10
  private val PhoneNumberMaxLength = 500
  private val MaxNumbers = 10
  private val MaxUsersToFind = 100
  private val DefaultMaxUserToFind = 10

  sealed trait UserUsecasesError extends RuntimeException
  final case class InternalError(msg: String) extends RuntimeException
  final case class NewUserDtoValidationError(
      errors: NonEmptyList[String]
  ) extends UserUsecasesError
  final case class QueryParamValidationError(
      errors: NonEmptyList[String]
  ) extends UserUsecasesError
  final case class UserIdDto(value: Int) extends AnyVal
  object UserIdDto {
    implicit val userIdDtoEncoder: Encoder[UserIdDto] = encodeUnwrapped
  }

  final case class EmailIdDto(value: Int) extends AnyVal
  object EmailIdDto {
    implicit val emailIdDtoEncoder: Encoder[EmailIdDto] = encodeUnwrapped
  }

  final case class MailDto(value: String) extends AnyVal
  object MailDto {
    implicit val mailDtoEncoder: Encoder[MailDto] = encodeUnwrapped
    implicit val mailDtoDecoder: Decoder[MailDto] = decodeUnwrapped

    def validate(value: MailDto): ValidationResults =
      Option(value.value).filter(_.nonNullOrEmpty) match {
        case None =>
          "mail cannot be empty".invalidNel.pure[NonEmptyList]
        case Some(mail) =>
          NonEmptyList.of(
            Validated.condNel(
              mail.length <= MailMaxLength,
              (),
              s"mail '$mail' is too long (max length $MailMaxLength)"
            )
          )
      }
  }

  final case class PhoneNumberIdDto(value: Int) extends AnyVal
  object PhoneNumberIdDto {
    implicit val phoneNumberIdDtoEncoder: Encoder[PhoneNumberIdDto] =
      encodeUnwrapped
  }

  final case class NumberDto(value: String) extends AnyVal
  object NumberDto {
    implicit val numberDtoEncoder: Encoder[NumberDto] = encodeUnwrapped
    implicit val numberDtoDecoder: Decoder[NumberDto] = decodeUnwrapped

    def validate(value: NumberDto): ValidationResults =
      Option(value.value).filter(_.nonNullOrEmpty) match {
        case None =>
          "number cannot be empty".invalidNel.pure[NonEmptyList]
        case Some(number) =>
          NonEmptyList.of(
            Validated.condNel(
              number.length <= LastNameMaxLength,
              (),
              s"number '$number' is too long (max length 500)"
            )
          )
      }
  }

  final case class FirstNameDto(value: String) extends AnyVal
  object FirstNameDto {
    implicit val firstNameDtoEncoder: Encoder[FirstNameDto] = encodeUnwrapped
    implicit val firstNameDtoDecoder: Decoder[FirstNameDto] = decodeUnwrapped

    def validate(value: FirstNameDto): ValidationResults =
      Option(value.value).filter(_.nonNullOrEmpty) match {
        case None =>
          "firstName cannot be empty".invalidNel.pure[NonEmptyList]
        case Some(lastName) =>
          NonEmptyList.of(
            Validated.condNel(
              lastName.length <= LastNameMaxLength,
              (),
              s"firstName can have a max length of $FirstNameMaxLength"
            )
          )
      }

    def toDomainF[F[_]](
        value: FirstNameDto
    )(implicit S: Sync[F]): F[FirstName] =
      validate(value).combineAll match {
        case Valid(_)        => FirstName(value.value).pure[F]
        case Invalid(errors) => S.raiseError(QueryParamValidationError(errors))
      }
  }

  final case class LastNameDto(value: String) extends AnyVal

  object LastNameDto {
    implicit val lastNameDtoEncoder: Encoder[LastNameDto] = encodeUnwrapped
    implicit val lastNameDtoDecoder: Decoder[LastNameDto] = decodeUnwrapped

    def validate(value: LastNameDto): ValidationResults =
      Option(value.value).filter(_.nonNullOrEmpty) match {
        case None =>
          "lastName cannot be empty".invalidNel.pure[NonEmptyList]
        case Some(lastName) =>
          NonEmptyList.of(
            Validated.condNel(
              lastName.length <= LastNameMaxLength,
              (),
              s"lastName can have a max length of $LastNameMaxLength"
            )
          )
      }

    def toDomainF[F[_]](value: LastNameDto)(implicit S: Sync[F]): F[LastName] =
      validate(value).combineAll match {
        case Valid(_)        => LastName(value.value).pure[F]
        case Invalid(errors) => S.raiseError(QueryParamValidationError(errors))
      }
  }

  final case class SearchLimitDto(value: Int) extends AnyVal
  object SearchLimitDto {
    private def validate(value: SearchLimitDto): ValidationResults =
      NonEmptyList.of(
        Validated.condNel(
          value.value >= 1 && value.value <= MaxUsersToFind,
          (),
          s"searchLimit must be between 1 and $MaxUsersToFind"
        )
      )

    def toDomainF[F[_]](
        value: Option[SearchLimitDto]
    )(implicit S: Sync[F]): F[SearchLimit] = value match {
      case None => SearchLimit(DefaultMaxUserToFind).pure[F]
      case Some(number) =>
        validate(number).combineAll match {
          case Valid(_) => SearchLimit(number.value).pure[F]
          case Invalid(errors) =>
            S.raiseError(QueryParamValidationError(errors))
        }
    }
  }

  final case class EmailWithIdDto(id: EmailIdDto, mail: MailDto)

  final case class PhoneNumberWithIdDto(
      id: PhoneNumberIdDto,
      number: NumberDto
  )

  final case class UserDto(
      id: UserIdDto,
      firstName: FirstNameDto,
      lastName: LastNameDto,
      emails: List[EmailWithIdDto],
      phoneNumbers: List[PhoneNumberWithIdDto]
  )

  object UserDto {
    implicit final class UserExtensions(val value: User) extends AnyVal {
      def toDto: UserDto = {
        import value._

        UserDto(
          UserIdDto(id.value),
          FirstNameDto(firstName.value),
          LastNameDto(lastName.value),
          emails.map(e =>
            EmailWithIdDto(EmailIdDto(e.id.value), MailDto(e.mail.value))
          ),
          phoneNumbers.map(p =>
            PhoneNumberWithIdDto(
              PhoneNumberIdDto(p.id.value),
              NumberDto(p.number.value)
            )
          )
        )
      }
    }
  }

  final case class UsersDto(users: List[UserDto])

  final case class NewUserDto(
      firstName: Option[FirstNameDto],
      lastName: Option[LastNameDto],
      emails: Option[Set[MailDto]],
      phoneNumbers: Option[Set[NumberDto]]
  )

  object NewUserDto {
    private final case class NewUserDtoRequired(
        firstName: FirstNameDto,
        lastName: LastNameDto,
        emails: Set[MailDto],
        phoneNumbers: Set[NumberDto]
    )

    implicit final class NewUserDtoExtensions(val value: NewUserDto)
        extends AnyVal {
      def toDomainF[F[_]: Sync]: F[NewUser] =
        for {
          _ <- validateNullabilityF
          domain <- toValidatedDomainF
        } yield domain

      private def validateNullabilityF[F[_]](implicit S: Sync[F]): F[Unit] = {
        def validateField[A](field: Option[A], name: String): ValidationResult =
          Validated.condNel(
            field.isDefined,
            (),
            s"$name must be present and not null"
          )
        NonEmptyList
          .of(
            validateField(value.lastName, "lastName"),
            validateField(value.firstName, "firstName"),
            validateField(value.emails, "emails"),
            validateField(value.phoneNumbers, "phoneNumbers")
          )
          .combineAll match {
          case Valid(_) => S.unit
          case Invalid(errors) =>
            S.raiseError(NewUserDtoValidationError(errors))
        }

      }
      private def toValidatedDomainF[F[_]: Sync]: F[NewUser] = {
        val S = Sync[F]

        val mapToRequired: F[NewUserDtoRequired] = (for {
          lastNameDto <- value.lastName
          firstNameDto <- value.firstName
          emailsDto <- value.emails
          phoneNumbersDto <- value.phoneNumbers
        } yield NewUserDtoRequired(
          firstNameDto,
          lastNameDto,
          emailsDto.filter(_.value.nonNullOrEmpty),
          phoneNumbersDto.filter(_.value.nonNullOrEmpty)
        ).pure[F])
          .getOrElse(
            S.raiseError(InternalError("nullability check not performed"))
          )

        for {
          newUserDtoRequired <- mapToRequired
          _ <- validateRequired(newUserDtoRequired)
        } yield {
          import newUserDtoRequired._

          NewUser(
            FirstName(firstName.value),
            LastName(lastName.value),
            emails.map(e => Mail(e.value)),
            phoneNumbers.map(p => Number(p.value))
          )
        }
      }

      private def validateRequired[F[_]: Sync](
          newUser: NewUserDtoRequired
      ): F[Unit] = {
        val S = Sync[F]
        val emailValidations = NonEmptyList.of(
          Validated.condNel(
            newUser.emails.nonEmpty,
            (),
            s"emails cannot be empty"
          ),
          Validated.condNel(
            newUser.emails.size <= MaxMails,
            (),
            s"emails can have a max size of $MaxMails"
          )
        ) ++ newUser.emails.toList.flatMap(e => MailDto.validate(e).toList)

        val phoneNumbersValidations = NonEmptyList.of(
          Validated.condNel(
            newUser.phoneNumbers.nonEmpty,
            (),
            s"phoneNumbers cannot be empty"
          ),
          Validated.condNel(
            newUser.phoneNumbers.size <= MaxNumbers,
            (),
            s"phoneNumbers can have a max size of $MaxMails"
          )
        ) ++ newUser.phoneNumbers.toList.flatMap(n =>
          NumberDto.validate(n).toList
        )

        val validationResults =
          LastNameDto.validate(newUser.lastName) ::: FirstNameDto.validate(
            newUser.firstName
          ) ::: emailValidations ::: phoneNumbersValidations

        validationResults.combineAll match {
          case Valid(_) => S.unit
          case Invalid(errors) =>
            S.raiseError(NewUserDtoValidationError(errors))
        }
      }
    }
  }
}
