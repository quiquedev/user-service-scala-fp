package info.quiquedev.userservice.usecases

import cats.ApplicativeError
import cats.effect.Async
import cats.implicits._
import doobie._
import doobie.implicits._
import info.quiquedev.userservice._
import info.quiquedev.userservice.usecases.model._

/**
  * Provides operations to manipulate users.
  * Effects are safely wrapped in F[_] which might return results
  * or raise errors.
  *
  * Note: errors are documented as `@throws` doc statements.
  */
trait UserUsecases[F[_]] {

  /**
    * creates a new user
    * @return user created
    */
  def createUser(newUser: NewUser): F[User]

  /**
    * find an user by the user id
    *
    * @return the found user or none if no user was found
    */
  def findUserById(userId: UserId): F[Option[User]]

  /**
    * finds a limited number of users by the last and the first name
    * the search is case insensitive
    *
    * @return the users found
    */
  def findUsersByName(
      firstName: FirstName,
      lastName: LastName,
      searchLimit: SearchLimit
  ): F[Set[User]]

  /**
    * delete a user by the user id
    *
    * @throws info.quiquedev.userservice.usecases.model.UserNotFoundError if the user does not exist
    * @throws info.quiquedev.userservice.usecases.model.DbError           if there was more than one user in the db
    */
  def deleteUserById(userId: UserId): F[Unit]

  /**
    * adds a mail to an existing user
    *
    * @return the updated user
    * @throws info.quiquedev.userservice.usecases.model.UserNotFoundError if the user does not exist
    * @throws info.quiquedev.userservice.usecases.model.TooManyMailsError if the user has already max number of mails
    */
  def addMailToUser(
      userId: UserId,
      mail: Mail
  ): F[User]

  /**
    * updates an existing mail of an user
    *
    * @return the updated user
    * @throws info.quiquedev.userservice.usecases.model.UserNotFoundError if the user does not exist
    * @throws info.quiquedev.userservice.usecases.model.MailNotFoundError if the mail does not exist
    */
  def updateMailFromUser(
      userId: UserId,
      mail: MailWithId
  ): F[User]

  /**
    * deletes an existing mail of an user
    *
    * @return the updated user
    * @throws info.quiquedev.userservice.usecases.model.UserNotFoundError   if the user does not exist
    * @throws info.quiquedev.userservice.usecases.model.MailNotFoundError   if the mail does not exist
    * @throws info.quiquedev.userservice.usecases.model.NotEnoughMailsError if there is just one mail left
    */
  def deleteMailFromUser(
      userId: UserId,
      mailId: MailId
  ): F[User]

  /**
    * adds a number to an existing user
    *
    * @return the updated user
    * @throws info.quiquedev.userservice.usecases.model.UserNotFoundError   if the user does not exist
    * @throws info.quiquedev.userservice.usecases.model.TooManyNumbersError if the user has already max number of numbers
    */
  def addNumberToUser(
      userId: UserId,
      number: Number
  ): F[User]

  /**
    * updates an existing number of an user
    *
    * @return the updated user
    * @throws info.quiquedev.userservice.usecases.model.UserNotFoundError   if the user does not exist
    * @throws info.quiquedev.userservice.usecases.model.NumberNotFoundError if the number does not exist
    */
  def updateNumberFromUser(
      userId: UserId,
      number: NumberWithId
  ): F[User]

  /**
    * deletes an existing number of an user
    *
    * @return the updated user
    * @throws info.quiquedev.userservice.usecases.model.UserNotFoundError     if the user does not exist
    * @throws info.quiquedev.userservice.usecases.model.NumberNotFoundError   if the number does not exist
    * @throws info.quiquedev.userservice.usecases.model.NotEnoughNumbersError if there is just one number left
    */
  def deleteNumberFromUser(
      userId: UserId,
      numberId: NumberId
  ): F[User]

}

object UserUsecases {
  def apply[F[_]](implicit ev: UserUsecases[F]) = ev

  def impl[F[_]: Async](implicit xa: Transactor[F]): UserUsecases[F] =
    new UserUsecases[F] {
      import MailWithId._
      import NumberWithId._

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
        def addMail(mails: MailsWithId): ConnectionIO[MailsWithId] =
          if (mails.size >= MaxMailsPerUser) CAE.raiseError(TooManyMailsError)
          else
            mails.find(_.mail == mail) match {
              case None =>
                val mailId = MailId(mails.maxBy(_.id.value).id.value + 1)
                (mails + MailWithId(mailId, mail)).pure[ConnectionIO]
              case Some(_) => mails.pure[ConnectionIO]
            }

        (for {
          mails <- userMails(userId)
          updatedMails <- addMail(mails)
          updateRequired = mails != updatedMails
          updatedUser <- if (updateRequired)
            updateUserMails(userId, updatedMails)
          else findUserOrFail(userId)
        } yield updatedUser).transact(xa)
      }

      def updateMailFromUser(userId: UserId, mail: MailWithId): F[User] = {
        def updateMail(mails: MailsWithId): ConnectionIO[MailsWithId] =
          mails.find(_.id == mail.id) match {
            case Some(oldMail) => (mails - oldMail + mail).pure[ConnectionIO]
            case None          => CAE.raiseError(MailNotFoundError)
          }

        (for {
          mails <- userMails(userId)
          updatedMails <- updateMail(mails)
          updatedUser <- updateUserMails(userId, updatedMails)
        } yield updatedUser).transact(xa)
      }

      def deleteMailFromUser(userId: UserId, mailId: MailId): F[User] = {
        def deleteMail(mails: MailsWithId): ConnectionIO[MailsWithId] =
          mails.find(_.id == mailId) match {
            case Some(mail) =>
              if (mails.size == 1) CAE.raiseError(NotEnoughMailsError)
              else (mails - mail).pure[ConnectionIO]
            case None => CAE.raiseError(MailNotFoundError)
          }

        (for {
          mails <- userMails(userId)
          updatedMails <- deleteMail(mails)
          updatedUser <- updateUserMails(userId, updatedMails)
        } yield updatedUser).transact(xa)
      }

      def addNumberToUser(userId: UserId, number: Number): F[User] = {
        def addNumber(
            numbers: NumbersWithId
        ): ConnectionIO[NumbersWithId] =
          if (numbers.size == MaxNumbersPerUser)
            CAE.raiseError(TooManyNumbersError)
          else
            numbers.find(_.number == number) match {
              case None =>
                val numberId = NumberId(numbers.maxBy(_.id.value).id.value + 1)
                (numbers + NumberWithId(numberId, number)).pure[ConnectionIO]
              case Some(_) => numbers.pure[ConnectionIO]
            }

        (for {
          numbers <- userNumbers(userId)
          updatedNumbers <- addNumber(numbers)
          updateRequired = numbers != updatedNumbers
          updatedUser <- if (updateRequired)
            updateUserNumbers(userId, updatedNumbers)
          else findUserOrFail(userId)
        } yield updatedUser).transact(xa)
      }

      def updateNumberFromUser(
          userId: UserId,
          number: NumberWithId
      ): F[User] = {
        def updateNumber(
            numbers: NumbersWithId
        ): ConnectionIO[NumbersWithId] =
          numbers.find(_.id == number.id) match {
            case Some(oldNumber) =>
              (numbers - oldNumber + number).pure[ConnectionIO]
            case None => CAE.raiseError(NumberNotFoundError)
          }

        (for {
          numbers <- userNumbers(userId)
          updatedNumbers <- updateNumber(numbers)
          updatedUser <- updateUserNumbers(userId, updatedNumbers)
        } yield updatedUser).transact(xa)
      }

      def deleteNumberFromUser(userId: UserId, numberId: NumberId): F[User] = {
        def deleteNumber(
            numbers: NumbersWithId
        ): ConnectionIO[NumbersWithId] =
          numbers.find(_.id == numberId) match {
            case Some(number) =>
              if (numbers.size == 1) CAE.raiseError(NotEnoughNumbersError)
              else (numbers - number).pure[ConnectionIO]
            case None => CAE.raiseError(NumberNotFoundError)
          }

        (for {
          numbers <- userNumbers(userId)
          updatedNumbers <- deleteNumber(numbers)
          updatedUser <- updateUserNumbers(userId, updatedNumbers)
        } yield updatedUser).transact(xa)
      }

      private def updateUserMails(
          userId: UserId,
          mails: MailsWithId
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

      private def userMails(userId: UserId): ConnectionIO[MailsWithId] =
        sql"""
          select emails
          from users
          where id = $userId
        """.query[MailsWithId].option.flatMap {
          case None        => CAE.raiseError(UserNotFoundError)
          case Some(mails) => mails.pure[ConnectionIO]
        }

      private def updateUserNumbers(
          userId: UserId,
          numbers: NumbersWithId
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

      private def userNumbers(userId: UserId): ConnectionIO[NumbersWithId] =
        sql"""
          select phone_numbers
          from users
           where id = $userId
        """.query[NumbersWithId].option.flatMap {
          case None        => CAE.raiseError(UserNotFoundError)
          case Some(mails) => mails.pure[ConnectionIO]
        }

      private def findUser(userId: UserId): ConnectionIO[Option[User]] = sql"""
          select *
          from users
          where id = $userId
        """.query[User].option

      private def findUserOrFail(userId: UserId): ConnectionIO[User] =
        findUser(userId).flatMap {
          case Some(user) => user.pure[ConnectionIO]
          case None       => CAE.raiseError(UserNotFoundError)
        }

    }
}
