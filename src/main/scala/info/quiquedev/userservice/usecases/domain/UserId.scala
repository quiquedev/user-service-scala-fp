package info.quiquedev.userservice.usecases.domain

import doobie.util.{Get, Put}

final case class UserId(value: Int) extends AnyVal

object UserId {
  implicit val userIdGet: Get[UserId] = Get[Int].map(UserId(_))
  implicit val userIdPut: Put[UserId] = Put[Int].contramap(_.value)
}