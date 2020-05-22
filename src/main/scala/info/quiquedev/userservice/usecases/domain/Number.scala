package info.quiquedev.userservice.usecases.domain

import io.circe.generic.extras.decoding.UnwrappedDecoder.decodeUnwrapped
import io.circe.generic.extras.encoding.UnwrappedEncoder.encodeUnwrapped
import io.circe.{Decoder, Encoder}

final case class Number(value: String) extends AnyVal

object Number {
  implicit val numberEncoder: Encoder[Number] = encodeUnwrapped
  implicit val numberDecoder: Decoder[Number] = decodeUnwrapped
}