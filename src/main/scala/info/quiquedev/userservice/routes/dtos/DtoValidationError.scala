package info.quiquedev.userservice.routes.dtos

import cats.data.NonEmptyList

sealed trait DtoValidationError extends RuntimeException
final case class InternalError(msg: String) extends RuntimeException
final case class NewUserDtoValidationError(
                                            errors: NonEmptyList[String]
                                          ) extends DtoValidationError
final case class QueryParamValidationError(
                                            errors: NonEmptyList[String]
                                          ) extends DtoValidationError
