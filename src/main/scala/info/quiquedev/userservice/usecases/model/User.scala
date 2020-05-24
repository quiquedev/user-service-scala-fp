package info.quiquedev.userservice.usecases.model

import info.quiquedev.userservice.usecases.model.MailWithId._
import info.quiquedev.userservice.usecases.model.NumberWithId._

final case class User(
    id: UserId,
    lastName: LastName,
    firstName: FirstName,
    emails: MailsWithId,
    phoneNumbers: NumbersWithId
)
