package info.quiquedev.userservice.usecases

import cats.data.NonEmptyList
import info.quiquedev.userservice.usecases.domain._

trait UserUsecases[F[_]] {
  def createUser(newUser: NewUser): F[User]
  def findUserById(userId: UserId): F[Option[User]]
  def findUsersByName(
      firstName: FirstName,
      lastName: LastName,
      searchLimit: SearchLimit
  ): F[List[User]]
  def deleteUserById(userId: UserId): F[Unit]
  def addMailToUser(
      userId: UserId,
      mail: Mail
  ): F[User]
  def updateMailFromUser(
      userId: UserId,
      mail: MailWithId
  ): F[User]
  def deleteMailFromUser(
      userId: UserId,
      mailId: MailId
  ): F[User]
  def addNumberToUser(
      userId: UserId,
      number: Number
  ): F[User]
  def updateNumberFromUser(
      userId: UserId,
      number: NumberWithId
  ): F[User]
  def deleteNumberFromUser(
      userId: UserId,
      numberId: NumberId
  ): F[User]

}

object UserUsecases {
  private type AdditionalData =
    (NonEmptyList[MailWithId], NonEmptyList[NumberWithId])

  def apply[F[_]](implicit ev: UserUsecases[F]) = ev
}
