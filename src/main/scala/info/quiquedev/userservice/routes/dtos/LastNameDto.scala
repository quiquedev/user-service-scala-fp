package info.quiquedev.userservice.routes.dtos

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated}
import cats.effect.Sync
import cats.implicits._
import info.quiquedev.userservice.usecases.domain.LastName
import io.circe.generic.extras.decoding.UnwrappedDecoder.decodeUnwrapped
import io.circe.generic.extras.encoding.UnwrappedEncoder.encodeUnwrapped
import io.circe.{Decoder, Encoder}

final case class LastNameDto(value: String) extends AnyVal

object LastNameDto {
  implicit val lastNameDtoEncoder: Encoder[LastNameDto] = encodeUnwrapped
  implicit val lastNameDtoDecoder: Decoder[LastNameDto] = decodeUnwrapped

  private[dtos] def validate(value: LastNameDto): ValidationResults =
    Option(value.value).filter(_.nonNullOrEmpty) match {
      case None =>
        "lastName cannot be empty".invalidNel.pure[NonEmptyList]
      case Some(lastName) =>
        NonEmptyList.of(
          Validated.condNel(
            lastName.length <= LastNameMaxLength,
            (),
            s"lastName can have a max length of $LastNameMaxLength"
          )
        )
    }

  def toDomainF[F[_]](value: LastNameDto)(implicit S: Sync[F]): F[LastName] =
    validate(value).combineAll match {
      case Valid(_)        => LastName(value.value).pure[F]
      case Invalid(errors) => S.raiseError(QueryParamValidationError(errors))
    }
  }
