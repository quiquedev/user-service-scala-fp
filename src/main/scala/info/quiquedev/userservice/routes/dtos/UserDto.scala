package info.quiquedev.userservice.routes.dtos

import info.quiquedev.userservice.usecases.model.User

final case class UserDto(
    id: UserIdDto,
    lastName: LastNameDto,
    firstName: FirstNameDto,
    emails: Set[MailWithIdDto],
    phoneNumbers: Set[NumberWithIdDto]
)

object UserDto {
  implicit final class UserExtensions(val value: User) extends AnyVal {
    def toDto: UserDto = {
      import value._

      UserDto(
        UserIdDto(id.value),
        LastNameDto(lastName.value),
        FirstNameDto(firstName.value),
        emails.map(e =>
          MailWithIdDto(MailIdDto(e.id.value), MailDto(e.mail.value))
        ),
        phoneNumbers.map(p =>
          NumberWithIdDto(
            NumberIdDto(p.id.value),
            NumberDto(p.number.value)
          )
        )
      )
    }
  }
}
