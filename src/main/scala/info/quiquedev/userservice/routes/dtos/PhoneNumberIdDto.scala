package info.quiquedev.userservice.routes.dtos

import io.circe.Encoder
import io.circe.generic.extras.encoding.UnwrappedEncoder.encodeUnwrapped

final case class PhoneNumberIdDto(value: Int) extends AnyVal

object PhoneNumberIdDto {
  implicit val phoneNumberIdDtoEncoder: Encoder[PhoneNumberIdDto] =
    encodeUnwrapped
}
