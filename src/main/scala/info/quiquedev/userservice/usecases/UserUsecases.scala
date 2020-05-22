package info.quiquedev.userservice.usecases

import cats.data.NonEmptyList
import info.quiquedev.userservice.usecases.domain.{
  NewUser,
  User,
  UserId,
  FirstName,
  LastName,
  SearchLimit,
  Mail,
  MailId,
  MailWithId,
  Number,
  NumberId,
  NumberWithId
}
import cats.implicits._
import java.time.Instant
import doobie._
import doobie.implicits._
import cats.effect.Async
import cats.ApplicativeError
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.parser._
import info.quiquedev.userservice.usecases.domain.UserNotFoundError
import info.quiquedev.userservice.usecases.domain._
import info.quiquedev.userservice.usecases.domain.DbError
import info.quiquedev.userservice.usecases._
import info.quiquedev.userservice._
import info.quiquedev.userservice.usecases.domain.TooManyMailsError
import info.quiquedev.userservice.usecases.domain.MailNotFoundError
import info.quiquedev.userservice.usecases.domain.TooManyNumbersError

trait UserUsecases[F[_]] {
  def createUser(newUser: NewUser): F[User]
  def findUserById(userId: UserId): F[Option[User]]
  def findUsersByName(
      firstName: FirstName,
      lastName: LastName,
      searchLimit: SearchLimit
  ): F[Set[User]]
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

  def impl[F[_]: Async](implicit xa: Transactor[F]): UserUsecases[F] =
    new UserUsecases[F] {
      val CAE = ApplicativeError[ConnectionIO, Throwable]

      def createUser(newUser: NewUser): F[User] = {
        import newUser._

        val mails = emails.zipWithIndex.map {
          case (mail, index) => MailWithId(MailId(index), mail)
        }

        val numbers = phoneNumbers.zipWithIndex.map {
          case (number, index) => NumberWithId(NumberId(index), number)
        }

        sql"""
            insert into users(last_name, first_name, emails, phone_numbers)
            values ($lastName, $firstName, $mails, $numbers)
        """.update
          .withUniqueGeneratedKeys[User](
            "id",
            "last_name",
            "first_name",
            "emails",
            "phone_numbers"
          )
          .transact(xa)
      }

      def findUserById(userId: UserId): F[Option[User]] =
        findUser(userId).transact(xa)

      def findUsersByName(
          firstName: FirstName,
          lastName: LastName,
          searchLimit: SearchLimit
      ): F[Set[User]] =
        sql"""
          select *
          from users
          where (lower(last_name) = lower($lastName) and lower(first_name) = lower($firstName))
          limit $searchLimit
        """.query[User].to[Set].transact(xa)

      def deleteUserById(userId: UserId): F[Unit] =
        sql"""delete from users where id = $userId""".update.run
          .flatMap {
            case 1 => CAE.unit
            case 0 => CAE.raiseError[Unit](UserNotFoundError)
            case _ =>
              CAE.raiseError[Unit](DbError(s"user with $userId was not unique"))
          }
          .transact(xa)

      def addMailToUser(userId: UserId, mail: Mail): F[User] = {
        def addMail(mails: Set[MailWithId]): ConnectionIO[Set[MailWithId]] =
          if (mails.size == MaxMailsPerUser) CAE.raiseError(TooManyMailsError)
          else {
            val mailId = MailId(mails.maxBy(_.id.value).id.value + 1)
            (mails + MailWithId(mailId, mail)).pure[ConnectionIO]
          }

        (for {
          mails <- userMails(userId)
          updatedMails <- addMail(mails)
          updatedUser <- updateUserMails(userId, updatedMails)
        } yield updatedUser).transact(xa)
      }

      def updateMailFromUser(userId: UserId, mail: MailWithId): F[User] = {
        def updateMail(mails: Set[MailWithId]): ConnectionIO[Set[MailWithId]] =
          mails.find(_.id == mail.id) match {
            case Some(_) => (mails + mail).pure[ConnectionIO]
            case None    => CAE.raiseError(MailNotFoundError)
          }

        (for {
          mails <- userMails(userId)
          updatedMails <- updateMail(mails)
          updatedUser <- updateUserMails(userId, updatedMails)
        } yield updatedUser).transact(xa)
      }

      def deleteMailFromUser(userId: UserId, mailId: MailId): F[User] = {
        def deleteMail(mails: Set[MailWithId]): ConnectionIO[Set[MailWithId]] =
          mails.find(_.id == mailId) match {
            case Some(mail) => (mails - mail).pure[ConnectionIO]
            case None       => CAE.raiseError(MailNotFoundError)
          }

        (for {
          mails <- userMails(userId)
          updatedMails <- deleteMail(mails)
          updatedUser <- updateUserMails(userId, updatedMails)
        } yield updatedUser).transact(xa)
      }

      def addNumberToUser(userId: UserId, number: Number): F[User] = {
        def addNumber(
            numbers: Set[NumberWithId]
        ): ConnectionIO[Set[NumberWithId]] =
          if (numbers.size == MaxNumbersPerUser)
            CAE.raiseError(TooManyNumbersError)
          else {
            val numberId = NumberId(numbers.maxBy(_.id.value).id.value + 1)
            (numbers + NumberWithId(numberId, number)).pure[ConnectionIO]
          }

        (for {
          numbers <- userNumbers(userId)
          updatedNumbers <- addNumber(numbers)
          updatedUser <- updateUserNumbers(userId, updatedNumbers)
        } yield updatedUser).transact(xa)
      }

      def updateNumberFromUser(
          userId: UserId,
          number: NumberWithId
      ): F[User] = {
        def updateNumber(
            numbers: Set[NumberWithId]
        ): ConnectionIO[Set[NumberWithId]] =
          numbers.find(_.id == number.id) match {
            case Some(_) => (numbers + number).pure[ConnectionIO]
            case None    => CAE.raiseError(NumberNotFoundError)
          }

        (for {
          numbers <- userNumbers(userId)
          updatedNumbers <- updateNumber(numbers)
          updatedUser <- updateUserNumbers(userId, updatedNumbers)
        } yield updatedUser).transact(xa)
      }

      def deleteNumberFromUser(userId: UserId, numberId: NumberId): F[User] = {
        def deleteNumber(
            numbers: Set[NumberWithId]
        ): ConnectionIO[Set[NumberWithId]] =
          numbers.find(_.id == numberId) match {
            case Some(number) => (numbers - number).pure[ConnectionIO]
            case None         => CAE.raiseError(NumberNotFoundError)
          }

        (for {
          numbers <- userNumbers(userId)
          updatedNumbers <- deleteNumber(numbers)
          updatedUser <- updateUserNumbers(userId, updatedNumbers)
        } yield updatedUser).transact(xa)
      }

      private def updateUserMails(
          userId: UserId,
          mails: Set[MailWithId]
      ): ConnectionIO[User] =
        sql"""
            update users
            set emails = $mails
            where id = $userId
          """.update.withUniqueGeneratedKeys[User](
          "id",
          "last_name",
          "first_name",
          "emails",
          "phone_numbers"
        )

      private def userMails(userId: UserId): ConnectionIO[Set[MailWithId]] =
        sql"""
          select email
          from users
           where id = ${userId}
        """".query[Set[MailWithId]].option.flatMap {
          case None        => CAE.raiseError(UserNotFoundError)
          case Some(mails) => mails.pure[ConnectionIO]
        }

      private def updateUserNumbers(
          userId: UserId,
          numbers: Set[NumberWithId]
      ): ConnectionIO[User] =
        sql"""
            update users
            set phone_numbers = $numbers
            where id = $userId
          """.update.withUniqueGeneratedKeys[User](
          "id",
          "last_name",
          "first_name",
          "emails",
          "phone_numbers"
        )

      private def userNumbers(userId: UserId): ConnectionIO[Set[NumberWithId]] =
        sql"""
          select phone_numbers
          from users
           where id = ${userId}
        """".query[Set[NumberWithId]].option.flatMap {
          case None        => CAE.raiseError(UserNotFoundError)
          case Some(mails) => mails.pure[ConnectionIO]
        }

      private def findUser(userId: UserId): ConnectionIO[Option[User]] = sql"""
          select *
          from users
          where id = $userId
        """.query[User].option

    }
}
