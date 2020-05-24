package info.quiquedev.userservice.routes.dtos

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated}
import cats.effect.Sync
import cats.implicits._
import info.quiquedev.userservice._
import info.quiquedev.userservice.usecases.model._

final case class NewUserDto(
    firstName: Option[FirstNameDto],
    lastName: Option[LastNameDto],
    emails: Option[Set[Option[MailDto]]],
    phoneNumbers: Option[Set[Option[NumberDto]]]
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
    def toModelF[F[_]: Sync]: F[NewUser] =
      for {
        _ <- validateNullabilityF
        model <- toValidatedModelF
      } yield model

    private def validateNullabilityF[F[_]](implicit S: Sync[F]): F[Unit] = {
      def validateNotNull[A](field: Option[A], name: String): ValidationResult =
        Validated.condNel(
          field.isDefined,
          (),
          s"$name must be present and not null"
        )

      NonEmptyList
        .of(
          validateNotNull(value.lastName, "lastName"),
          validateNotNull(value.firstName, "firstName"),
          validateNotNull(value.emails, "emails"),
          validateNotNull(value.phoneNumbers, "phoneNumbers")
        )
        .combineAll match {
        case Valid(_) => S.unit
        case Invalid(errors) =>
          S.raiseError(RequestBodyValidationError(errors))
      }
    }

    private def toValidatedModelF[F[_]: Sync]: F[NewUser] = {
      val S = Sync[F]

      val mapToRequired: F[NewUserDtoRequired] = (for {
        lastNameDto <- value.lastName
        firstNameDto <- value.firstName
        emailsDto <- value.emails
        phoneNumbersDto <- value.phoneNumbers
      } yield {
        val nonEmptyMails = emailsDto.toList.flatten.toSet
        val nonEmptyNumbers = phoneNumbersDto.toList.flatten.toSet

        NewUserDtoRequired(
          firstNameDto,
          lastNameDto,
          nonEmptyMails
            .filter(_.value.nonNullOrEmpty),
          nonEmptyNumbers.filter(_.value.nonNullOrEmpty)
        ).pure[F]
      }).getOrElse(
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
          newUser.emails.size <= MaxMailsPerUser,
          (),
          s"emails can have a max size of $MaxMailsPerUser"
        )
      ) ++ newUser.emails.toList.flatMap(e => MailDto.validate(e).toList)

      val phoneNumbersValidations = NonEmptyList.of(
        Validated.condNel(
          newUser.phoneNumbers.nonEmpty,
          (),
          s"phoneNumbers cannot be empty"
        ),
        Validated.condNel(
          newUser.phoneNumbers.size <= MaxNumbersPerUser,
          (),
          s"phoneNumbers can have a max size of $MaxMailsPerUser"
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
          S.raiseError(RequestBodyValidationError(errors))
      }
    }
  }
}
