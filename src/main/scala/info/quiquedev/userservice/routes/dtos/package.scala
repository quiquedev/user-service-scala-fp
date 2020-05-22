package info.quiquedev.userservice.routes

import cats.data.{NonEmptyList, Validated}

package object dtos {
  type ValidationResult = Validated[NonEmptyList[String], Unit]
  type ValidationResults = NonEmptyList[ValidationResult]

  val FirstNameMaxLength = 500
  val LastNameMaxLength = 500
  val MailMaxLength = 500
  val MaxMails = 10
  val PhoneNumberMaxLength = 500
  val MaxNumbers = 10
  val MaxSearchLimit = 100
  val DefaultSearchLimit = 10

  implicit final class StringExtensions(val value: String) extends AnyVal {
    def nonNullOrEmpty: Boolean = Option(value).forall(!_.trim.isEmpty)
  }
}
