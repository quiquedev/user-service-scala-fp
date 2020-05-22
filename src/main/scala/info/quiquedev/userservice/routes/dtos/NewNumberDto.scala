package info.quiquedev.userservice.routes.dtos

import cats.data.{NonEmptyList, Validated}
import cats.implicits._
import io.circe.generic.extras.decoding.UnwrappedDecoder.decodeUnwrapped
import io.circe.generic.extras.encoding.UnwrappedEncoder.encodeUnwrapped
import io.circe.{Decoder, Encoder}
import cats.effect.Sync
import info.quiquedev.userservice.usecases.domain.Number
import cats.data.Validated._

final case class NewNumberDto(value: Option[NumberDto])

object NewNumberDto {
  implicit final class newNumberDtoExtensions(val value: NewNumberDto)
      extends AnyVal {
    def toDomainF[F[_]](implicit S: Sync[F]): F[Number] = value.value match {
      case None =>
        S.raiseError(
          RequestBodyValidationError(
            "value must be present and not null".pure[NonEmptyList]
          )
        )
      case Some(numberDto) =>
        NumberDto.validate(numberDto).combineAll match {
          case Valid(_) => Number(numberDto.value).pure[F]
          case Invalid(errors) =>
            S.raiseError(RequestBodyValidationError(errors))
        }
    }
  }
}
