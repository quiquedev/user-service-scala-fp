package info.quiquedev.userservice.usecases.domain

import io.circe.generic.auto._

final case class NumberWithId(id: NumberId, number: Number)

object NumberWithId {
  type NumbersWithId = Set[NumberWithId]

  implicit val numbersWithIdSetJsonCoder = jsonCoderOf[NumbersWithId]
}
