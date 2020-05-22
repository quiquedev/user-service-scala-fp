package info.quiquedev.userservice.usecases.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.decoding.UnwrappedDecoder.decodeUnwrapped
import io.circe.generic.extras.encoding.UnwrappedEncoder.encodeUnwrapped

final case class EmailId(value: Int) extends AnyVal

object EmailId {
  implicit val emailIdEncoder: Encoder[EmailId] = encodeUnwrapped
  implicit val emailIdDecoder: Decoder[EmailId] = decodeUnwrapped
}

