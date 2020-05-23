package info.quiquedev.userservice.usecases.domain

import MailWithId._
import NumberWithId._

final case class User(
    id: UserId,
    lastName: LastName,
    firstName: FirstName,
    emails: MailsWithId,
    phoneNumbers: NumbersWithId 
)