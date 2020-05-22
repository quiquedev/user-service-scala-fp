package info.quiquedev.userservice.usecases.domain

sealed trait UserUsecasesError extends RuntimeException
final case object UserNotFoundError extends UserUsecasesError