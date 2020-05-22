package info.quiquedev.userservice.routes.dtos

import io.circe.Encoder
import io.circe.generic.extras.encoding.UnwrappedEncoder.encodeUnwrapped

final case class MailIdDto(value: Int) extends AnyVal

  object MailIdDto {
    implicit val mailIdDtoEncoder: Encoder[MailIdDto] = encodeUnwrapped
  }