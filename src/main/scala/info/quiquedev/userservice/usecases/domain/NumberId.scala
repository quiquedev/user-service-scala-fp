package info.quiquedev.userservice.usecases.domain

import io.circe.generic.extras.decoding.UnwrappedDecoder.decodeUnwrapped
import io.circe.generic.extras.encoding.UnwrappedEncoder.encodeUnwrapped
import io.circe.{Decoder, Encoder}

final case class NumberId(value: Int) extends AnyVal

object NumberId {
  implicit val numberIdEncoder: Encoder[NumberId] = encodeUnwrapped
  implicit val numberIdDecoder: Decoder[NumberId] = decodeUnwrapped
}