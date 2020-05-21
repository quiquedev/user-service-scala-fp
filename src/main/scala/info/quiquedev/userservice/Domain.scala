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

object Domain {
  sealed trait UserUsecasesError extends RuntimeException

  final case class UserId(value: Int) extends AnyVal
  object UserId {
    implicit val userIdGet: Get[UserId] = Get[Int].map(UserId(_))
    implicit val userIdPut: Put[UserId] = Put[Int].contramap(_.value)
  }

  final case class EmailId(value: Int) extends AnyVal
  object EmailId {
    implicit val emailIdGet: Get[EmailId] = Get[Int].map(EmailId(_))
    implicit val emailIdPut: Put[EmailId] = Put[Int].contramap(_.value)

    implicit val emailIdEncoder: Encoder[EmailId] = encodeUnwrapped
    implicit val emailIdDecoder: Decoder[EmailId] = decodeUnwrapped
  }

  final case class PhoneNumberId(value: Int) extends AnyVal
  object PhoneNumberId {
    implicit val phoneNumberIdGet: Get[PhoneNumberId] =
      Get[Int].map(PhoneNumberId(_))
    implicit val phoneNumberIdPut: Put[PhoneNumberId] =
      Put[Int].contramap(_.value)

    implicit val phoneNumberIdEncoder: Encoder[PhoneNumberId] = encodeUnwrapped
    implicit val phoneNumberIdDecoder: Decoder[PhoneNumberId] = decodeUnwrapped
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

  final case class PhoneNumber(id: EmailId, value: String)

  final case class User(
      id: UserId,
      firstName: FirstName,
      lastName: LastName,
      emails: List[Email],
      phoneNumbers: List[PhoneNumber]
  )
}
