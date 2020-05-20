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
  def apply[F[_]](implicit ev: UserUsecases[F]) = ev

  def impl[F[_]: Async](implicit xa: Transactor[F]): UserUsecases[F] =
    new UserUsecases[F] {
      private val CAE = ApplicativeError[ConnectionIO, Throwable]

      override def createUser(user: User): F[User] = {
        import user._

        lazy val insertUser: ConnectionIO[Unit] = sql"""
          insert into users
          values($id, $firstName, $lastName)
        """.update.run.verifyUpdate(
          s"user $user could not be inserted in table 'users'"
        )

        lazy val insertEmails: ConnectionIO[Unit] =
          emails
            .map(email => insertEmail(id, email))
            .toList
            .sequence
            .map(_ => ())

        lazy val insertPhoneNumbers: ConnectionIO[Unit] =
          phoneNumbers
            .map(phoneNumber => insertPhoneNumber(id, phoneNumber))
            .toList
            .sequence
            .map(_ => ())

        (for {
          _ <- verifyAvailableUserId(id)
          _ <- insertUser
          _ <- insertEmails
          _ <- insertPhoneNumbers
        } yield user).transact(xa)
      }

      override def findUserById(userId: UserId): F[Option[User]] = {
        val maybeUserData: ConnectionIO[Option[(UserId, FirstName, LastName)]] =
          sql"select first_name, last_name from users where user_id = $userId"
            .query[(FirstName, LastName)]
            .option
            .map {
              _.map {
                case (firstName, lastName) => (userId, firstName, lastName)
              }
            }

        loadUser(maybeUserData)
      }

      override def findUserByName(
          firstName: FirstName,
          lastName: LastName
      ): F[Option[NonEmptyList[User]]] = {
        val maybeUserData: ConnectionIO[Option[(UserId, FirstName, LastName)]] =
          sql"""
            select user_id
            from users
            where lower(first_name) = lower($firstName) and lower(last_name) = lower($lastName) 
          """
            .query[UserId]
            .option
            .map {
              _.map(userId => (userId, firstName, lastName))
            }

        loadUser(maybeUserData)
      }

      override def addEmailToUser(
          userId: UserId,
          email: Email
      ): F[(UserId, NonEmptyList[Email])] = ???

      override def updateEmailFromUser(
          userId: UserId,
          email: Email
      ): F[(UserId, NonEmptyList[Email])] = ???

      override def addPhoneNumberToUser(
          userId: UserId,
          phoneNumber: PhoneNumber
      ): F[(UserId, NonEmptyList[PhoneNumber])] = ???

      override def updatePhoneNumberFromUser(
          userId: UserId,
          phoneNumber: PhoneNumber
      ): F[(UserId, NonEmptyList[PhoneNumber])] = ???

      private def existsUserId(userId: UserId): ConnectionIO[Boolean] = ???
      private def verifyAvailableUserId(userId: UserId): ConnectionIO[Unit] =
        ???
      private def verifyExistingUserId(userId: UserId): ConnectionIO[Unit] = ???
      private def insertEmail(
          userId: UserId,
          email: Email
      ): ConnectionIO[Unit] =
        ???
      private def insertPhoneNumber(
          userId: UserId,
          phoneNumber: PhoneNumber
      ): ConnectionIO[Unit] =
        ???

      private def findEmailsForUser(
          userId: UserId
      ): ConnectionIO[NonEmptyList[Email]] =
        sql"""
          select email_id, email
          from emails
          where user_id = $userId = userId 
        """.query[Email].nel

      private def findPhoneNumbersForUser(
          userId: UserId
      ): ConnectionIO[NonEmptyList[PhoneNumber]] =
        sql"""
          select phone_number_id, phone_number
          from phone_numbers
          where user_id = $userId = userId 
        """.query[PhoneNumber].nel

      private def loadUser(
          maybeUserData: ConnectionIO[Option[(UserId, FirstName, LastName)]]
      ): F[Option[User]] = {
        def findEmailsAndPhoneNumbers(userId: UserId) =
          for {
            emails <- findEmailsForUser(userId)
            phoneNumbers <- findPhoneNumbersForUser(userId)
          } yield (emails, phoneNumbers)

        val result: ConnectionIO[Option[User]] = for {
          maybeUserData <- maybeUserData
          maybeUser <- maybeUserData match {
            case None => none[User].pure[ConnectionIO]
            case Some((userId, firstName, lastName)) =>
              findEmailsAndPhoneNumbers(userId).map {
                case (emails, phoneNumbers) =>
                  User(userId, firstName, lastName, emails, phoneNumbers).some
              }
          }
        } yield maybeUser

        result.transact(xa)
      }
    }

  private implicit final class ConnectionIOUpdatesExtensions(
      val value: ConnectionIO[Int]
  ) extends AnyVal {
    def verifyUpdate(errorMsg: String)(
        implicit CAE: ApplicativeError[ConnectionIO, Throwable]
    ): ConnectionIO[Unit] = value.flatMap {
      case 1 => CAE.unit
      case _ =>
        CAE.raiseError(
          DatabaseError(errorMsg)
        )
    }
  }
}
