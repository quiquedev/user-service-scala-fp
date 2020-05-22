package info.quiquedev.userservice.routes.dtos

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated}
import cats.effect.Sync
import cats.implicits._
import info.quiquedev.userservice.usecases.domain.SearchLimit

final case class SearchLimitDto(value: Int) extends AnyVal

object SearchLimitDto {
  private def validate(value: SearchLimitDto): ValidationResults =
    NonEmptyList.of(
      Validated.condNel(
        value.value >= 1 && value.value <= MaxSearchLimit,
        (),
        s"searchLimit must be between 1 and $MaxSearchLimit"
      )
    )

  def toDomainF[F[_]](
                       value: Option[SearchLimitDto]
                     )(implicit S: Sync[F]): F[SearchLimit] = value match {
    case None => SearchLimit(DefaultSearchLimit).pure[F]
    case Some(number) =>
      validate(number).combineAll match {
        case Valid(_) => SearchLimit(number.value).pure[F]
        case Invalid(errors) =>
          S.raiseError(QueryParamValidationError(errors))
      }
  }
}

