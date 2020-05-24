package info.quiquedev.userservice.usecases.model

import doobie.util.{Get, Put}

final case class LastName(value: String) extends AnyVal

object LastName {
  implicit val lastNameGet: Get[LastName] = Get[String].map(LastName(_))
  implicit val lastNamePut: Put[LastName] = Put[String].contramap(_.value)

}
