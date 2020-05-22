package info.quiquedev.userservice.routes.dtos

import cats.data.{NonEmptyList, Validated}
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.decoding.UnwrappedDecoder.decodeUnwrapped
import io.circe.generic.extras.encoding.UnwrappedEncoder.encodeUnwrapped
import info.quiquedev.userservice.routes._
import cats.implicits._

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
