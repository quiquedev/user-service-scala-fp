package info.quiquedev.userservice.usecases.domain

final case class User(
    id: UserId,
    lastName: LastName,
    firstName: FirstName,
    emails: List[MailWithId],
    phoneNumbers: List[NumberWithId]
)
