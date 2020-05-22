package info.quiquedev.userservice.usecases.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.decoding.UnwrappedDecoder.decodeUnwrapped
import io.circe.generic.extras.encoding.UnwrappedEncoder.encodeUnwrapped

final case class PhoneNumberId(value: Int) extends AnyVal

object PhoneNumberId {
  implicit val phoneNumberIdEncoder: Encoder[PhoneNumberId] = encodeUnwrapped
  implicit val phoneNumberIdDecoder: Decoder[PhoneNumberId] = decodeUnwrapped
}