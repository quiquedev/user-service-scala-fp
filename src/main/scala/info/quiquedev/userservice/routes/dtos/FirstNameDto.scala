package info.quiquedev.userservice.routes.dtos

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated}
import cats.effect.Sync
import cats.implicits._
import info.quiquedev.userservice.usecases.domain.FirstName
import io.circe.generic.extras.decoding.UnwrappedDecoder.decodeUnwrapped
import io.circe.generic.extras.encoding.UnwrappedEncoder.encodeUnwrapped
import io.circe.{Decoder, Encoder}

final case class FirstNameDto(value: String) extends AnyVal

object FirstNameDto {
  implicit val firstNameDtoEncoder: Encoder[FirstNameDto] = encodeUnwrapped
  implicit val firstNameDtoDecoder: Decoder[FirstNameDto] = decodeUnwrapped

  private[dtos] def validate(value: FirstNameDto): ValidationResults =
    Option(value.value).filter(_.nonNullOrEmpty) match {
      case None =>
        "firstName cannot be empty".invalidNel.pure[NonEmptyList]
      case Some(lastName) =>
        NonEmptyList.of(
          Validated.condNel(
            lastName.length <= LastNameMaxLength,
            (),
            s"firstName can have a max length of $FirstNameMaxLength"
          )
        )
    }

  def toDomainF[F[_]](
                       value: FirstNameDto
                     )(implicit S: Sync[F]): F[FirstName] =
    validate(value).combineAll match {
      case Valid(_)        => FirstName(value.value).pure[F]
      case Invalid(errors) => S.raiseError(QueryParamValidationError(errors))
    }
}
