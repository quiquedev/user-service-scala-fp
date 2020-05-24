package info.quiquedev.userservice.usecases.model

import io.circe.generic.auto._

final case class NumberWithId(id: NumberId, number: Number)

object NumberWithId {
  type NumbersWithId = Set[NumberWithId]

  implicit val numbersWithIdSetJsonCoder = jsonCoderOf[NumbersWithId]
}
