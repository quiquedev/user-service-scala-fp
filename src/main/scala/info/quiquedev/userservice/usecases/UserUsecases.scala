package info.quiquedev.userservice.usecases

import cats.data.NonEmptyList
import domain._

trait UserUsecases[F[_]] {
  def createUser(newUser: NewUser): F[User]
  def findUserById(userId: UserId): F[Option[User]]
  def findUsersByName(
      firstName: FirstName,
      lastName: LastName,
      searchLimit: SearchLimit
  ): F[List[User]]
  def deleteUserById(userId: UserId): F[Unit]
  def addEmailToUser(
      userId: UserId,
      email: MailWithId
  ): F[(UserId, NonEmptyList[MailWithId])]
  def updateEmailFromUser(
      userId: UserId,
      email: MailWithId
  ): F[(UserId, NonEmptyList[MailWithId])]
  def addPhoneNumberToUser(
      userId: UserId,
      phoneNumber: NumberWithId
  ): F[(UserId, NonEmptyList[NumberWithId])]
  def updatePhoneNumberFromUser(
      userId: UserId,
      phoneNumber: NumberWithId
  ): F[(UserId, NonEmptyList[NumberWithId])]
}

object UserUsecases {
  private type AdditionalData = (NonEmptyList[MailWithId], NonEmptyList[NumberWithId])

  def apply[F[_]](implicit ev: UserUsecases[F]) = ev
}
