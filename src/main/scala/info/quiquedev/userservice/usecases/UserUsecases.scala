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
      email: Email
  ): F[(UserId, NonEmptyList[Email])]
  def updateEmailFromUser(
      userId: UserId,
      email: Email
  ): F[(UserId, NonEmptyList[Email])]
  def addPhoneNumberToUser(
      userId: UserId,
      phoneNumber: PhoneNumber
  ): F[(UserId, NonEmptyList[PhoneNumber])]
  def updatePhoneNumberFromUser(
      userId: UserId,
      phoneNumber: PhoneNumber
  ): F[(UserId, NonEmptyList[PhoneNumber])]
}

object UserUsecases {
  private type AdditionalData = (NonEmptyList[Email], NonEmptyList[PhoneNumber])

  def apply[F[_]](implicit ev: UserUsecases[F]) = ev
}
