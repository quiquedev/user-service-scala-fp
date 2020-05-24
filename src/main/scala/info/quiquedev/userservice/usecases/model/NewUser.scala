package info.quiquedev.userservice.usecases.model

final case class NewUser(
    firstName: FirstName,
    lastName: LastName,
    emails: Set[Mail],
    phoneNumbers: Set[Number]
)
