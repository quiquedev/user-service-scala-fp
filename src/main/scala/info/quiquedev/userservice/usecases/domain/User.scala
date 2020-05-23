package info.quiquedev.userservice.usecases.domain

import info.quiquedev.userservice.usecases.domain.MailWithId._
import info.quiquedev.userservice.usecases.domain.NumberWithId._

final case class User(
    id: UserId,
    lastName: LastName,
    firstName: FirstName,
    emails: MailsWithId,
    phoneNumbers: NumbersWithId
)
