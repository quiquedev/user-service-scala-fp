package info.quiquedev.userservice.routes.dtos

import cats.data.{NonEmptyList, Validated}
import cats.implicits._
import io.circe.generic.extras.decoding.UnwrappedDecoder.decodeUnwrapped
import io.circe.generic.extras.encoding.UnwrappedEncoder.encodeUnwrapped
import io.circe.{Decoder, Encoder}
import cats.effect.Sync
import info.quiquedev.userservice.usecases.domain.Mail
import cats.data.Validated._
final case class NewMailDto(value: Option[MailDto])

object NewMailDto {
  implicit final class newMailDtoExtensions(val value: NewMailDto)
      extends AnyVal {
    def toDomainF[F[_]](implicit S: Sync[F]): F[Mail] = value.value match {
      case None =>
        S.raiseError(
          RequestBodyValidationError(
            "value must be present and not null".pure[NonEmptyList]
          )
        )
      case Some(mailDto) =>
        MailDto.validate(mailDto).combineAll match {
          case Valid(_) => Mail(mailDto.value).pure[F]
          case Invalid(errors) =>
            S.raiseError(RequestBodyValidationError(errors))
        }
    }
  }
}
