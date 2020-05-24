package info.quiquedev.userservice.usecases.model

import doobie.util.{Get, Put}

final case class FirstName(value: String) extends AnyVal

object FirstName {
  implicit val firstNameGet: Get[FirstName] = Get[String].map(FirstName(_))
  implicit val firstNamePut: Put[FirstName] = Put[String].contramap(_.value)
}
