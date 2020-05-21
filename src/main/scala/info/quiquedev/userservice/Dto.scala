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
import org.http4s.Uri.UserInfo

object Dto {
  private type ValidationResult = Validated[NonEmptyList[String], Unit]
  private type ValidationResults = NonEmptyList[ValidationResult]

  private val FirstNameMaxLength = 500
  private val LastNameMaxLength = 500
  private val EmailMaxLength = 500
  private val PhoneNumberMaxLength = 500

  sealed trait UserUsecasesError extends RuntimeException
  final case class NewUserDtoValidationError(
      newUserDto: NewUserDto,
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

  final case class EmailDto(value: String) extends AnyVal
  object EmailDto {
    implicit val emailIdDtoEncoder: Encoder[EmailDto] = encodeUnwrapped
  }

  final case class PhoneNumberIdDto(value: Int) extends AnyVal
  object PhoneNumberIdDto {
    implicit val phoneNumberIdDtoEncoder: Encoder[PhoneNumberIdDto] =
      encodeUnwrapped
  }

  final case class PhoneNumberDto(value: String) extends AnyVal
  object PhoneNumberDto {
    implicit val phoneNumberDtoEncoder: Encoder[PhoneNumberDto] =
      encodeUnwrapped
  }

  final case class FirstNameDto(value: String) extends AnyVal
  object FirstNameDto {
    implicit val firstNameDtoEncoder: Encoder[FirstNameDto] = encodeUnwrapped
  }

  final case class LastNameDto(value: String) extends AnyVal
  object LastNameDto {
    implicit val firstNameDtoEncoder: Encoder[FirstNameDto] = encodeUnwrapped
  }

  final case class EmailWithIdDto(id: EmailIdDto, mail: EmailDto)

  final case class PhoneNumberWithIdDto(
      id: PhoneNumberIdDto,
      number: PhoneNumberDto
  )

  final case class UserDto(
      id: UserIdDto,
      firstName: FirstNameDto,
      lastName: LastNameDto,
      emails: List[EmailWithIdDto],
      phoneNumbers: List[PhoneNumberWithIdDto]
  )

  final case class NewUserDto(
      id: UserIdDto,
      firstName: FirstNameDto,
      lastName: LastNameDto,
      emails: List[EmailDto],
      phoneNumbers: List[PhoneNumberDto]
  )

  object NewUserDto {
    implicit final class NewUserDtoExtensions(val value: NewUserDto)
        extends AnyVal {

      def validateF[F[_]](implicit S: Sync[F]): F[Unit] =
        for {
          _ <- validateUserNullFieldsF(value)
        } yield ()
      private def validateUserNullFieldsF[F[_]](
          value: NewUserDto
      )(implicit S: Sync[F]): F[Unit] = {
        def validateField[A](
            value: A,
            name: String
        ): ValidationResult =
          Validated.condNel(
            Option(value).isDefined,
            (),
            s"$name cannot be null"
          )

        val validatedNel: ValidationResults = NonEmptyList.of(
          validateField(value.id, "id"),
          validateField(value.firstName, "firstName"),
          validateField(value.lastName, "lastName"),
          validateField(value.emails, "emails"),
          validateField(value.phoneNumbers, "phoneNumbers")
        )

        validatedNel.combineAll match {
          case Valid(_) => S.unit
          case Invalid(errors) =>
            S.raiseError(NewUserDtoValidationError(value, errors))
        }
      }
    }
  }
}
