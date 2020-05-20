package info.quiquedev.userservice.usecases

import cats.data.NonEmptyList
import UserUsecasesDomain._

trait UserUsecases[F[_]] {
  def createUser(user: User): F[User]
  def findUserById(userId: UserId): F[Option[User]]
  def findUserByName(
      firstName: FirstName,
      lastName: LastName
  ): F[Option[NonEmptyList[User]]]
  def addEmailsToUser(
      userId: UserId,
      emails: NonEmptyList[Email]
  ): F[(UserId, NonEmptyList[Email])]
  def updateEmailFromUser(
      userId: UserId,
      email: Email
  ): F[(UserId, NonEmptyList[Email])]
  def addPhoneNumbersToUser(
      userId: UserId,
      phoneNumbers: NonEmptyList[PhoneNumber]
  ): F[(UserId, NonEmptyList[PhoneNumber])]
  def updatePhoneNumberFromUser(
      userId: UserId,
      phoneNumber: PhoneNumber
  ): F[(UserId, NonEmptyList[PhoneNumber])]
}
