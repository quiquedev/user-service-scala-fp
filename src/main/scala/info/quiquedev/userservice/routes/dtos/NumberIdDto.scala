package info.quiquedev.userservice.routes.dtos

import io.circe.Encoder
import io.circe.generic.extras.encoding.UnwrappedEncoder.encodeUnwrapped

final case class NumberIdDto(value: Int) extends AnyVal

object NumberIdDto {
  implicit val numberIdDtoEncoder: Encoder[NumberIdDto] =
    encodeUnwrapped
}
