package info.quiquedev.userservice.usecases

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.data.Validated
import info.quiquedev.userservice._
import cats.implicits._
import cats.data.Validated._


object UserUsecasesDomain {
  type ValidationResult = Validated[NonEmptyList[String], Unit]
  type ValidationResults = NonEmptyList[ValidationResult]

  private val FirstNameMaxLength = 500
  private val LastNameMaxLength = 500
  private val EmailMaxLength = 500
  private val PhoneNumberMaxLength = 500

  sealed trait UserUsecasesError extends RuntimeException
  final case class FirstNameValidationError(
      firstName: FirstName,
      error: String
  ) extends UserUsecasesError
  final case class LastNameValidationError(
      lastName: LastName,
      error: String
  ) extends UserUsecasesError
  final case class EmailValidationError(
      email: Email,
      errors: NonEmptyList[String]
  ) extends UserUsecasesError
  final case class PhoneNumberValidationError(
      phoneNumber: PhoneNumber,
      errors: NonEmptyList[String]
  ) extends UserUsecasesError
  final case class UserValidationError(user: User, errors: NonEmptyList[String])
      extends UserUsecasesError

  final case class UserId(value: Int) extends AnyVal
  final case class EmailId(value: Int) extends AnyVal
  final case class PhoneNumberId(value: Int) extends AnyVal

  final case class FirstName(value: String) extends AnyVal
  private object FirstName {
    def validate(value: FirstName): ValidationResults =
      Validated
        .condNel(
          value.value.nonNullOrEmpty && value.value.length <= FirstNameMaxLength,
          (),
          s"firstName must be a non empty string with a max length of $FirstNameMaxLength"
        )
        .pure[NonEmptyList]
  }
  final case class LastName(value: String) extends AnyVal

  private object LastName {
    def validate(value: LastName): ValidationResults =
      Validated
        .condNel(
          value.value.nonNullOrEmpty && value.value.length <= LastNameMaxLength,
          (),
          s"lastName must be a non empty string with a max length of $LastNameMaxLength"
        )
        .pure[NonEmptyList]
  }

  final case class Email(id: EmailId, value: String)
  object Email {
    implicit final class EmailExtensions(val value: Email) extends AnyVal {
      def validate: ValidationResults =
        NonEmptyList.of(
          Validated
            .condNel(Option(value.id).isDefined, (), "email id cannot be null"),
          Validated.condNel(
            value.value.nonNullOrEmpty && value.value.length <= EmailMaxLength,
            (),
            s"email must be a non empty string with max length of $EmailMaxLength"
          )
        )

      def validateF[F[_]](implicit S: Sync[F]): F[Unit] = {
        validate.combineAll match {
          case Valid(_) => S.unit
          case Invalid(errors) =>
            S.raiseError(EmailValidationError(value, errors))
        }
      }
    }
  }

  final case class PhoneNumber(id: EmailId, value: String)
  object PhoneNumber {
    implicit final class PhoneNumberExtensions(val value: PhoneNumber)
        extends AnyVal {
      def validate: ValidationResults =
        NonEmptyList.of(
          Validated.condNel(
            Option(value.id).isDefined,
            (),
            "phone number id cannot be null"
          ),
          Validated.condNel(
            value.value.nonNullOrEmpty && value.value.length <= PhoneNumberMaxLength,
            (),
            s"phone number must be a non empty string with a max length of $PhoneNumberMaxLength"
          )
        )

      def validateF[F[_]](implicit S: Sync[F]): F[Unit] = {
        validate.combineAll match {
          case Valid(_) => S.unit
          case Invalid(errors) =>
            S.raiseError(PhoneNumberValidationError(value, errors))
        }
      }
    }
  }

  final case class User(
      id: UserId,
      firstName: FirstName,
      lastName: LastName,
      emails: Option[NonEmptyList[Email]],
      phoneNumbers: Option[NonEmptyList[PhoneNumber]]
  )

  object User {
    implicit final class UserExtensions(val value: User) extends AnyVal {
      private def validateNullFields[F[_]](implicit S: Sync[F]): F[Unit] = {
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
            S.raiseError(UserValidationError(value, errors))
        }
      }

      private def validateFieldContents[F[_]](implicit S: Sync[F]): F[Unit] = {
        def validateListField[A](
            field: Option[NonEmptyList[A]]
        )(validation: A => ValidationResults): ValidationResults =
          field match {
            case None => ().validNel.pure[NonEmptyList]
            case Some(content) =>
              content.flatMap(validation)
          }

        val firstNameValidationResults = FirstName.validate(value.firstName)
        val lastNameValidationResults = LastName.validate(value.lastName)
        val emailsValidationResults =
          validateListField(value.emails)(_.validate)
        val phoneNumbersValidationResults =
          validateListField(value.phoneNumbers)(_.validate)

        (firstNameValidationResults ::: lastNameValidationResults ::: emailsValidationResults ::: phoneNumbersValidationResults).combineAll match {
          case Valid(_) => S.unit
          case Invalid(errors) =>
            S.raiseError(UserValidationError(value, errors))
        }
      }

      def validate[F[_]](implicit S: Sync[F]): F[Unit] =
        for {
          _ <- validateNullFields
          _ <- validateFieldContents
        } yield ()
    }
  }
}
