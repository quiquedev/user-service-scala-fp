package info.quiquedev.userservice.routes.dtos

import cats.data.{NonEmptyList, Validated}
import cats.implicits._
import io.circe.generic.extras.decoding.UnwrappedDecoder.decodeUnwrapped
import io.circe.generic.extras.encoding.UnwrappedEncoder.encodeUnwrapped
import io.circe.{Decoder, Encoder}

final case class NumberDto(value: String) extends AnyVal

object NumberDto {
  implicit val numberDtoEncoder: Encoder[NumberDto] = encodeUnwrapped
  implicit val numberDtoDecoder: Decoder[NumberDto] = decodeUnwrapped

  private[dtos] def validate(value: NumberDto): ValidationResults =
    Option(value.value).filter(_.nonNullOrEmpty) match {
      case None =>
        "number cannot be empty".invalidNel.pure[NonEmptyList]
      case Some(number) =>
        NonEmptyList.of(
          Validated.condNel(
            number.length <= PhoneNumberMaxLength,
            (),
            s"number '$number' is too long (max length 500)"
          )
        )
    }
}