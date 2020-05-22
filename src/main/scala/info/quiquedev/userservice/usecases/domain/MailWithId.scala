package info.quiquedev.userservice.usecases.domain

import io.circe.generic.auto._

final case class MailWithId(id: MailId, mail: Mail)

object MailWithId {
  implicit val mailWithIdSetJsonCoder = jsonCoderOf[Set[MailWithId]]
}
