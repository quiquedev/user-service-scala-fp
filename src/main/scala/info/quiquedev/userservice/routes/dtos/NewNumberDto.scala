package info.quiquedev.userservice.routes.dtos

import cats.data.NonEmptyList
import cats.data.Validated._
import cats.effect.Sync
import cats.implicits._
import info.quiquedev.userservice.usecases.model.Number

final case class NewNumberDto(value: Option[NumberDto])

object NewNumberDto {
  implicit final class newNumberDtoExtensions(val value: NewNumberDto)
      extends AnyVal {
    def toModelF[F[_]](implicit S: Sync[F]): F[Number] = value.value match {
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
