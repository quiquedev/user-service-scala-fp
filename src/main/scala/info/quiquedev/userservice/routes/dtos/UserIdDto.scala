package info.quiquedev.userservice.routes.dtos

import io.circe.Encoder
import io.circe.generic.extras.encoding.UnwrappedEncoder.encodeUnwrapped

final case class UserIdDto(value: Int) extends AnyVal

object UserIdDto {
  implicit val userIdDtoEncoder: Encoder[UserIdDto] = encodeUnwrapped
}
