package info.quiquedev.userservice.usecases

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.data.Validated
import info.quiquedev.userservice._
import cats.implicits._
import doobie.util.Get
import doobie.util.Put
import cats.data.Validated._

object UserUsecasesDomain {
  import DomainValidation._

  sealed trait UserUsecasesError extends RuntimeException
  final case class UserValidationError(user: User, errors: NonEmptyList[String])
      extends UserUsecasesError
  final case class EmailValidationError(
      email: Email,
      errors: NonEmptyList[String]
  ) extends UserUsecasesError
  final case class PhoneNumberValidationError(
      phoneNumber: PhoneNumber,
      errors: NonEmptyList[String]
  ) extends UserUsecasesError

  final case class UserId(value: Int) extends AnyVal
  object UserId {
    implicit val userIdGet: Get[UserId] = Get[Int].map(UserId(_))
    implicit val userIdPut: Put[UserId] = Put[Int].contramap(_.value)
  }

  final case class EmailId(value: Int) extends AnyVal
  object EmailId {
    implicit val emailIdGet: Get[EmailId] = Get[Int].map(EmailId(_))
    implicit val emailIdPut: Put[EmailId] = Put[Int].contramap(_.value)
  }

  final case class PhoneNumberId(value: Int) extends AnyVal
  object PhoneNumberId {
    implicit val phoneNumberIdGet: Get[PhoneNumberId] =
      Get[Int].map(PhoneNumberId(_))
    implicit val phoneNumberIdPut: Put[PhoneNumberId] =
      Put[Int].contramap(_.value)
  }

  final case class FirstName(value: String) extends AnyVal
  object FirstName {
    implicit val firstNameGet: Get[FirstName] = Get[String].map(FirstName(_))
    implicit val firstNamePut: Put[FirstName] = Put[String].contramap(_.value)
  }
  final case class LastName(value: String) extends AnyVal

  object LastName {
    implicit val lastNameGet: Get[LastName] = Get[String].map(LastName(_))
    implicit val lastNamePut: Put[LastName] = Put[String].contramap(_.value)

  }

  final case class Email(id: EmailId, value: String)
  object Email {
    implicit final class EmailExtensions(val value: Email) extends AnyVal {
      def validateF[F[_]](implicit S: Sync[F]): F[Unit] = {
        validateEmail(value).combineAll match {
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
      def validateF[F[_]](implicit S: Sync[F]): F[Unit] = {
        validatePhoneNumber(value).combineAll match {
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

      def validateF[F[_]](implicit S: Sync[F]): F[Unit] =
        for {
          _ <- validateUserNullFieldsF(value)
          _ <- validateUserFieldContentsF(value)
        } yield ()
    }
  }

  private object DomainValidation {
    private val FirstNameMaxLength = 500
    private val LastNameMaxLength = 500
    private val EmailMaxLength = 500
    private val PhoneNumberMaxLength = 500

    type ValidationResult = Validated[NonEmptyList[String], Unit]
    type ValidationResults = NonEmptyList[ValidationResult]

    def validateFirstName(value: FirstName): ValidationResults =
      Validated
        .condNel(
          value.value.nonNullOrEmpty && value.value.length <= FirstNameMaxLength,
          (),
          s"firstName must be a non empty string with a max length of $FirstNameMaxLength"
        )
        .pure[NonEmptyList]

    def validateLastName(value: LastName): ValidationResults =
      Validated
        .condNel(
          value.value.nonNullOrEmpty && value.value.length <= LastNameMaxLength,
          (),
          s"lastName must be a non empty string with a max length of $LastNameMaxLength"
        )
        .pure[NonEmptyList]

    def validateEmail(value: Email): ValidationResults =
      NonEmptyList.of(
        Validated
          .condNel(Option(value.id).isDefined, (), "email id cannot be null"),
        Validated.condNel(
          value.value.nonNullOrEmpty && value.value.length <= EmailMaxLength,
          (),
          s"email must be a non empty string with max length of $EmailMaxLength"
        )
      )

    def validatePhoneNumber(value: PhoneNumber): ValidationResults =
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

    def validateUserNullFieldsF[F[_]](
        value: User
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
          S.raiseError(UserValidationError(value, errors))
      }
    }

    def validateUserFieldContentsF[F[_]](
        value: User
    )(implicit S: Sync[F]): F[Unit] = {
      def validateListField[A](
          field: Option[NonEmptyList[A]]
      )(validation: A => ValidationResults): ValidationResults =
        field match {
          case None => ().validNel.pure[NonEmptyList]
          case Some(content) =>
            content.flatMap(validation)
        }

      val firstNameValidationResults = validateFirstName(value.firstName)
      val lastNameValidationResults = validateLastName(value.lastName)
      val emailsValidationResults =
        validateListField(value.emails)(validateEmail)
      val phoneNumbersValidationResults =
        validateListField(value.phoneNumbers)(validatePhoneNumber)

      (firstNameValidationResults ::: lastNameValidationResults ::: emailsValidationResults ::: phoneNumbersValidationResults).combineAll match {
        case Valid(_) => S.unit
        case Invalid(errors) =>
          S.raiseError(UserValidationError(value, errors))
      }
    }

  }
}
