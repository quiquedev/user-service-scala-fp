package info.quiquedev.userservice.routes.dtos

import io.circe.Encoder
import io.circe.generic.extras.encoding.UnwrappedEncoder.encodeUnwrapped

final case class EmailIdDto(value: Int) extends AnyVal

  object EmailIdDto {
    implicit val emailIdDtoEncoder: Encoder[EmailIdDto] = encodeUnwrapped
  }