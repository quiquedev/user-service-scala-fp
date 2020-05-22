package info.quiquedev.userservice.routes.dtos

final case class PhoneNumberWithIdDto(
                                       id: PhoneNumberIdDto,
                                       number: NumberDto
                                     )
