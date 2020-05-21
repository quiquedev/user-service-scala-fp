package info.quiquedev.userservice

import cats.implicits._
import cats.data.NonEmptyList
import cats.effect.Async
import doobie.util.transactor.Transactor
import cats.ApplicativeError
import doobie._
import doobie.implicits._
import Domain._
import Dto._

trait UserUsecases[F[_]] {
  def createUser(newUser: NewUser): F[User]
  def findUserById(userId: UserId): F[Option[User]]
  def findUsersByName(
      firstName: FirstName,
      lastName: LastName,
      searchLimit: SearchLimit
  ): F[List[User]]
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