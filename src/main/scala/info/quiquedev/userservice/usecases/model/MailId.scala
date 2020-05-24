package info.quiquedev.userservice.usecases.model

import io.circe.generic.extras.decoding.UnwrappedDecoder.decodeUnwrapped
import io.circe.generic.extras.encoding.UnwrappedEncoder.encodeUnwrapped
import io.circe.{Decoder, Encoder}

final case class MailId(value: Int) extends AnyVal

object MailId {
  implicit val mailIdEncoder: Encoder[MailId] = encodeUnwrapped
  implicit val mailIdDecoder: Decoder[MailId] = decodeUnwrapped
}
