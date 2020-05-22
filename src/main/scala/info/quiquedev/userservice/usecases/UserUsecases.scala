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
  import JsonDoobie._

  private type AdditionalData =
    (NonEmptyList[MailWithId], NonEmptyList[NumberWithId])

  def apply[F[_]](implicit ev: UserUsecases[F]) = ev

  def impl[F[_]: Async](implicit xa: Transactor[F]): UserUsecases[F] =
    new UserUsecases[F] {
      def createUser(newUser: NewUser): F[User] = {
        import newUser._

        val mails = emails.zipWithIndex.map {
          case (mail, index) => MailWithId(MailId(index), mail)
        }

        val numbers = phoneNumbers.zipWithIndex.map {
          case (number, index) => NumberWithId(NumberId(index), number)
        }

        sql"""
            insert into users(lastName, firstName, emails, phoneNumbers)
            values ($lastName, $firstName, $mails, $numbers)
        """.update
          .withUniqueGeneratedKeys[User](
            "id",
            "firstName",
            "lastName",
            "emails",
            "phoneNumbers"
          )
          .transact(xa)
      }

      def findUserById(userId: UserId): F[Option[User]] = ???

      def findUsersByName(
          firstName: FirstName,
          lastName: LastName,
          searchLimit: SearchLimit
      ): F[List[User]] = ???

      def deleteUserById(userId: UserId): F[Unit] = ???

      def addMailToUser(userId: UserId, mail: Mail): F[User] = ???

      def updateMailFromUser(userId: UserId, mail: MailWithId): F[User] = ???

      def deleteMailFromUser(userId: UserId, mailId: MailId): F[User] = ???

      def addNumberToUser(userId: UserId, number: Number): F[User] = ???

      def updateNumberFromUser(userId: UserId, number: NumberWithId): F[User] =
        ???

      def deleteNumberFromUser(userId: UserId, numberId: NumberId): F[User] =
        ???

    }
}

private object JsonDoobie {
  import org.postgresql.util.PGobject

  implicit val jsonMeta: Meta[Json] =
    Meta.Advanced
      .other[PGobject]("json")
      .timap[Json](a => parse(a.getValue).leftMap[Json](e => throw e).merge)(
        a => {
          val o = new PGobject
          o.setType("json")
          o.setValue(a.noSpaces)
          o
        }
      )
}
