package info.quiquedev.userservice.routes.dtos

import cats.data.{NonEmptyList, Validated}
import cats.data.Validated.{Invalid, Valid}
import cats.effect.Sync
import info.quiquedev.userservice.usecases.domain.{FirstName, LastName, Mail, NewUser, Number}
import cats.implicits._

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
