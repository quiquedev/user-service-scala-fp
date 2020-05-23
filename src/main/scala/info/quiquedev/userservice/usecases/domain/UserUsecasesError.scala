package info.quiquedev.userservice.usecases.domain

sealed trait UserUsecasesError extends RuntimeException

final case object UserNotFoundError extends UserUsecasesError
final case object MailNotFoundError extends UserUsecasesError
final case object NumberNotFoundError extends UserUsecasesError
final case object TooManyMailsError extends UserUsecasesError
final case object NotEnoughMailsError extends UserUsecasesError
final case object TooManyNumbersError extends UserUsecasesError
final case object NotEnoughNumbers extends UserUsecasesError
final case class DbJsonCodingError(t: Throwable) extends UserUsecasesError
final case class DbError(msg: String) extends UserUsecasesError
