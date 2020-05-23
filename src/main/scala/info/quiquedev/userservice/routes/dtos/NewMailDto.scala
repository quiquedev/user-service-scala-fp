package info.quiquedev.userservice.routes.dtos

import cats.data.NonEmptyList
import cats.data.Validated._
import cats.effect.Sync
import cats.implicits._
import info.quiquedev.userservice.usecases.domain.Mail
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
