package info.quiquedev.userservice.routes.dtos

import info.quiquedev.userservice.usecases.domain.User

final case class UserDto(
                          id: UserIdDto,
                          firstName: FirstNameDto,
                          lastName: LastNameDto,
                          emails: List[EmailWithIdDto],
                          phoneNumbers: List[PhoneNumberWithIdDto]
                        )

object UserDto {
  implicit final class UserExtensions(val value: User) extends AnyVal {
    def toDto: UserDto = {
      import value._

      UserDto(
        UserIdDto(id.value),
        FirstNameDto(firstName.value),
        LastNameDto(lastName.value),
        emails.map(e =>
          EmailWithIdDto(EmailIdDto(e.id.value), MailDto(e.mail.value))
        ),
        phoneNumbers.map(p =>
          PhoneNumberWithIdDto(
            PhoneNumberIdDto(p.id.value),
            NumberDto(p.number.value)
          )
        )
      )
    }
  }
}
