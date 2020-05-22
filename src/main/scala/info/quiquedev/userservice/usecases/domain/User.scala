package info.quiquedev.userservice.usecases.domain

final case class User(
                       id: UserId,
                       firstName: FirstName,
                       lastName: LastName,
                       emails: List[Email],
                       phoneNumbers: List[PhoneNumber]
                     )
