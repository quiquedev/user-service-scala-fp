package info.quiquedev.userservice.usecases

import cats.implicits._
import cats.data.NonEmptyList
import UserUsecasesDomain._
import cats.effect.Async
import doobie.util.transactor.Transactor
import cats.ApplicativeError
import doobie._
import doobie.implicits._
import cats.data.OptionT

trait UserUsecases[F[_]] {
  def createUser(user: User): F[User]
  def findUserById(userId: UserId): F[Option[User]]
  def findUserByName(
      firstName: FirstName,
      lastName: LastName
  ): F[Option[NonEmptyList[User]]]
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

  def impl[F[_]: Async](implicit xa: Transactor[F]): UserUsecases[F] =
    new UserUsecases[F] {
      private val CAE = ApplicativeError[ConnectionIO, Throwable]

      def createUser(user: User): F[User] = ???

      def findUserById(userId: UserId): F[Option[User]] = ???

      def findUserByName(
          firstName: FirstName,
          lastName: LastName
      ): F[Option[NonEmptyList[User]]] = ???

      def addEmailToUser(userId: UserId, email: Email): F[
        (UserId, NonEmptyList[Email])
      ] = ???

      def updateEmailFromUser(
          userId: UserId,
          email: Email
      ): F[(UserId, NonEmptyList[Email])] = ???

      def addPhoneNumberToUser(
          userId: UserUsecasesDomain.UserId,
          phoneNumber: UserUsecasesDomain.PhoneNumber
      ): F[(UserId, NonEmptyList[PhoneNumber])] = ???

      def updatePhoneNumberFromUser(
          userId: UserId,
          phoneNumber: PhoneNumber
      ): F[(UserId, NonEmptyList[PhoneNumber])] = ???

    }

  // private implicit final class ConnectionIOUpdatesExtensions(
  //     val value: ConnectionIO[Int]
  // ) extends AnyVal {
  //   def verifyUpdate(errorMsg: String)(
  //       implicit CAE: ApplicativeError[ConnectionIO, Throwable]
  //   ): ConnectionIO[Unit] = value.flatMap {
  //     case 1 => CAE.unit
  //     case _ =>
  //       CAE.raiseError(
  //         DatabaseError(errorMsg)
  //       )
    // }
  // }
}
