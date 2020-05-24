package info.quiquedev.userservice.usecases.model

import io.circe.generic.extras.decoding.UnwrappedDecoder.decodeUnwrapped
import io.circe.generic.extras.encoding.UnwrappedEncoder.encodeUnwrapped
import io.circe.{Decoder, Encoder}

final case class Mail(value: String) extends AnyVal

object Mail {
  implicit val mailEncoder: Encoder[Mail] = encodeUnwrapped
  implicit val mailDecoder: Decoder[Mail] = decodeUnwrapped
}
