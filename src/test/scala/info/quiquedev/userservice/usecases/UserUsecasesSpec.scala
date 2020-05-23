package info.quiquedev.userservice.usecases

import cats.implicits._
import org.scalatest.wordspec.AnyWordSpec
import cats.effect.IO
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.ContainerDef
import org.scalatest.matchers.should.Matchers
import org.scalatest.OptionValues
import doobie.util.transactor.Transactor
import doobie._
import doobie.implicits._
import doobie.implicits.legacy.instant._
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
  NumberWithId,
  UserUsecasesError,
  UserNotFoundError
}
import info.quiquedev.userservice.usecases.domain.TooManyMailsError
import info.quiquedev.userservice.usecases.domain.MailNotFoundError
import info.quiquedev.userservice.usecases.domain.NotEnoughMailsError
import info.quiquedev.userservice.usecases.domain.TooManyNumbersError
import info.quiquedev.userservice.usecases.domain.NumberNotFoundError
import info.quiquedev.userservice.usecases.domain.NotEnoughNumbersError

class UserUsecasesSpec
    extends AnyWordSpec
    with Matchers
    with OptionValues
    with TestContainerForAll {
  import UserUsecases._

  private implicit var xa: Transactor[IO] = null
  private var usecases: UserUsecases[IO] = null

  override val containerDef: ContainerDef =
    PostgreSQLContainer.Def("postgres:12")

  override def afterContainersStart(container: Containers): Unit = {
    super.afterContainersStart(container)

    container match {
      case pgContainer: PostgreSQLContainer => {
        import pgContainer._

        xa = FlywayMigrator.migrateDbAndGetTransactorIO(
          jdbcUrl,
          username,
          password
        )
        usecases = UserUsecases.impl[IO]
      }
    }
  }

  override def beforeTest(container: Containers): Unit =
    sql"""
    delete from users
  """.update.run.map(_ => ()).transact(xa).unsafeRunSync()

  "UserUsecases" should {
    "provide with a createUser method to create users" which {
      "returns user created" in {
        // given
        val newUser = NewUser(
          FirstName("enrique"),
          LastName("molina"),
          Set(Mail("enrique@gmail.com")),
          Set(Number("12345"))
        )

        // when
        val result = usecases.createUser(newUser).unsafeRunSync()

        // then
        result.firstName should be(newUser.firstName)
        result.lastName should be(newUser.lastName)
        result.emails.map(_.mail) should contain theSameElementsAs (newUser.emails)
        result.phoneNumbers.map(_.number) should contain theSameElementsAs newUser.phoneNumbers
      }
    }

    "provide with a findById method to find users by id" which {
      "returns the existing user" in {
        // given
        val existingUser = insertUserInDb(
          NewUser(
            FirstName("enrique"),
            LastName("molina"),
            Set(Mail("enrique@gmail.com")),
            Set(Number("12345"))
          )
        )

        // when
        val result =
          usecases.findUserById(existingUser.id).unsafeRunSync()

        // then
        result.value should be(existingUser)
      }

      "returns nothing if the user cannot be found" in {
        // given
        val unexistingUser = UserId(1)

        // when
        val result =
          usecases.findUserById(unexistingUser).unsafeRunSync()

        // then
        result should be(None)
      }
    }

    "provide with a findByName method to find users by last and first name" which {
      "returns the limited amount users" in {
        // given
        val searchLimit = SearchLimit(2)
        val existingUsers = Set(
          insertUserInDb(
            NewUser(
              FirstName("enrique"),
              LastName("molina"),
              Set(Mail("enrique@gmail.com")),
              Set(Number("12345"))
            )
          ),
          insertUserInDb(
            NewUser(
              FirstName("Enrique"),
              LastName("MOLINA"),
              Set(Mail("enrique@yahoo.com")),
              Set(Number("1234"))
            )
          ),
          insertUserInDb(
            NewUser(
              FirstName("ENRIQUE"),
              LastName("MOLINA"),
              Set(Mail("enrique@yahoo.es")),
              Set(Number("1234"))
            )
          )
        )

        insertUserInDb(
          NewUser(
            FirstName("Enrique"),
            LastName("benitez"),
            Set(Mail("enrique@lala.com")),
            Set(Number("1"))
          )
        )

        // when
        val result =
          usecases
            .findUsersByName(
              FirstName("enrique"),
              LastName("molina"),
              searchLimit
            )
            .unsafeRunSync()

        // then
        result.intersect(existingUsers).size should be(searchLimit.value)
      }

      "returns empty result if there was no match" in {
        // given
        val unexistingUser = UserId(1)

        // when
        val result =
          usecases
            .findUsersByName(
              FirstName("enrique"),
              LastName("molina"),
              SearchLimit(5)
            )
            .unsafeRunSync()

        // then
        result shouldBe empty
      }
    }

    "provide with a deleteUserById method" which {
      "deletes an existing user" in {
        // given
        val existingUser = insertUserInDb(
          NewUser(
            FirstName("enrique"),
            LastName("molina"),
            Set(Mail("enrique@gmail.com")),
            Set(Number("12345"))
          )
        )

        // when
        usecases.deleteUserById(existingUser.id).unsafeRunSync()

        // then
        findUserInDb(existingUser.id) should be(None)
      }

      "fails if the user does not exist" in {
        // given
        val unexistingUserId = UserId(1)

        // then
        an[UserNotFoundError.type] shouldBe thrownBy(
          usecases.deleteUserById(unexistingUserId).unsafeRunSync()
        )
      }
    }

    "provide with a addMailToUser method to add a mail to user" which {
      "returns the updated user" in {
        // given
        val user = insertUserInDb(
          NewUser(
            FirstName("enrique"),
            LastName("molina"),
            Set(Mail("enrique@gmail.com")),
            Set(Number("55555"))
          )
        )

        val newMail = Mail("enrique@yahoo.com")

        // when
        val result = usecases.addMailToUser(user.id, newMail).unsafeRunSync()

        // then
        val expectedUpdatedUser =
          user.copy(emails = user.emails + MailWithId(MailId(1), newMail))

        result should be(expectedUpdatedUser)
        findUserInDb(user.id).value should be(expectedUpdatedUser)
      }

      "fails if the user does not exist" in {
        // given
        val nonExistingUserId = UserId(1)
        val newMail = Mail("enrique@yahoo.com")

        // then
        an[UserNotFoundError.type] should be thrownBy usecases
          .addMailToUser(nonExistingUserId, newMail)
          .unsafeRunSync()
      }

      "fails if the user has already too many mails" in {
        // given
        val user = insertUserInDb(
          NewUser(
            FirstName("enrique"),
            LastName("molina"),
            (0 until 10).map(i => Mail(s"mail$i@gmail.com")).toSet,
            Set(Number("55555"))
          )
        )

        val newMail = Mail("enrique@yahoo.com")

        // then
        an[TooManyMailsError.type] should be thrownBy usecases
          .addMailToUser(user.id, newMail)
          .unsafeRunSync()
      }
    }

    "provide with a updateMailFromUser method to update an existing mail of an user" which {
      "returns the updated user" in {
        // given
        val user = insertUserInDb(
          NewUser(
            FirstName("enrique"),
            LastName("molina"),
            Set(Mail("enrique@gmail.com")),
            Set(Number("55555"))
          )
        )

        val newMailWithId =
          user.emails.head.copy(mail = Mail("enrique@yahoo.com"))

        // when
        val result =
          usecases.updateMailFromUser(user.id, newMailWithId).unsafeRunSync()

        // then
        val expectedUpdatedUser =
          user.copy(emails = Set(newMailWithId))

        result should be(expectedUpdatedUser)
        findUserInDb(user.id).value should be(expectedUpdatedUser)
      }

      "fails if the user does not exist" in {
        // given
        val nonExistingUserId = UserId(1)
        val newMailWithId = MailWithId(MailId(1), Mail("enrique@yahoo.com"))

        // then
        an[UserNotFoundError.type] should be thrownBy usecases
          .updateMailFromUser(nonExistingUserId, newMailWithId)
          .unsafeRunSync()
      }

      "fails if the mail does not exist" in {
        // given
        val user = insertUserInDb(
          NewUser(
            FirstName("enrique"),
            LastName("molina"),
            Set(Mail("enrique@gmail.com")),
            Set(Number("55555"))
          )
        )

        val newMailWithNonExistingId =
          MailWithId(MailId(5), Mail("enrique@yahoo.com"))

        // then
        an[MailNotFoundError.type] should be thrownBy usecases
          .updateMailFromUser(user.id, newMailWithNonExistingId)
          .unsafeRunSync()

      }
    }

    "provide with a deleteMailFromUser method to delete an existing mail from an user" which {
      "returns the updated user" in {
        // given
        val user = insertUserInDb(
          NewUser(
            FirstName("enrique"),
            LastName("molina"),
            Set(Mail("enrique@yahoo.com"), Mail("enrique@gmail.com")),
            Set(Number("55555"))
          )
        )

        val mailWithIdToDelete = user.emails.head

        // when
        val result =
          usecases
            .deleteMailFromUser(user.id, mailWithIdToDelete.id)
            .unsafeRunSync()

        // then
        val expectedUpdatedUser =
          user.copy(emails = user.emails - mailWithIdToDelete)

        result should be(expectedUpdatedUser)
        findUserInDb(user.id).value should be(expectedUpdatedUser)
      }

      "fails if the user does not exist" in {
        // given
        val nonExistingUserId = UserId(1)
        val mailIdToDelete = MailId(1)

        // then
        an[UserNotFoundError.type] should be thrownBy usecases
          .deleteMailFromUser(nonExistingUserId, mailIdToDelete)
          .unsafeRunSync()
      }

      "fails if the mail does not exist" in {
        // given
        val user = insertUserInDb(
          NewUser(
            FirstName("enrique"),
            LastName("molina"),
            Set(Mail("enrique@yahoo.com")),
            Set(Number("55555"))
          )
        )

        val mailIdToDelete = MailId(25)

        // then
        an[MailNotFoundError.type] should be thrownBy usecases
          .deleteMailFromUser(user.id, mailIdToDelete)
          .unsafeRunSync()
      }

      "fails if the user only has one mail left" in {
        // given
        val user = insertUserInDb(
          NewUser(
            FirstName("enrique"),
            LastName("molina"),
            Set(Mail("enrique@yahoo.com")),
            Set(Number("55555"))
          )
        )

        val mailIdToDelete = user.emails.head.id

        // then
        an[NotEnoughMailsError.type] should be thrownBy usecases
          .deleteMailFromUser(user.id, mailIdToDelete)
          .unsafeRunSync()
      }
    }

    "provide with a addNumberToUser method to add a number to an user" which {
      "returns the updated user" in {
        // given
        val user = insertUserInDb(
          NewUser(
            FirstName("enrique"),
            LastName("molina"),
            Set(Mail("enrique@gmail.com")),
            Set(Number("55555"))
          )
        )

        val newNumber = Number("66666")

        // when
        val result =
          usecases.addNumberToUser(user.id, newNumber).unsafeRunSync()

        // then
        val expectedUpdatedUser =
          user.copy(phoneNumbers =
            user.phoneNumbers + NumberWithId(NumberId(1), newNumber)
          )

        result should be(expectedUpdatedUser)
        findUserInDb(user.id).value should be(expectedUpdatedUser)
      }

      "fails if the user does not exist" in {
        // given
        val nonExistingUserId = UserId(1)
        val newNumber = Number("55555")

        // then
        an[UserNotFoundError.type] should be thrownBy usecases
          .addNumberToUser(nonExistingUserId, newNumber)
          .unsafeRunSync()
      }

      "fails if the user has already too many numbers" in {
        // given
        val user = insertUserInDb(
          NewUser(
            FirstName("enrique"),
            LastName("molina"),
            Set(Mail("enrique@gmail.com")),
            (0 until 10).map(i => Number(i.toString)).toSet
          )
        )

        val newNumber = Number("20")

        // then
        an[TooManyNumbersError.type] should be thrownBy usecases
          .addNumberToUser(user.id, newNumber)
          .unsafeRunSync()
      }
    }

    "provide with a updateNumberFromUser method to update an existing number of an user" which {
      "returns the updated user" in {
        // given
        val user = insertUserInDb(
          NewUser(
            FirstName("enrique"),
            LastName("molina"),
            Set(Mail("enrique@gmail.com")),
            Set(Number("55555"))
          )
        )

        val newNumberWithId =
          user.phoneNumbers.head.copy(number = Number("11111"))

        // when
        val result =
          usecases
            .updateNumberFromUser(user.id, newNumberWithId)
            .unsafeRunSync()

        // then
        val expectedUpdatedUser =
          user.copy(phoneNumbers = Set(newNumberWithId))

        result should be(expectedUpdatedUser)
        findUserInDb(user.id).value should be(expectedUpdatedUser)
      }

      "fails if the user does not exist" in {
        // given
        val nonExistingUserId = UserId(1)
        val newNumberWithId = NumberWithId(NumberId(1), Number("55555"))

        // then
        an[UserNotFoundError.type] should be thrownBy usecases
          .updateNumberFromUser(nonExistingUserId, newNumberWithId)
          .unsafeRunSync()
      }

      "fails if the number does not exist" in {
        // given
        val user = insertUserInDb(
          NewUser(
            FirstName("enrique"),
            LastName("molina"),
            Set(Mail("enrique@gmail.com")),
            Set(Number("55555"))
          )
        )

        val newNumberWithNonExistingId =
          NumberWithId(NumberId(5), Number("88888"))

        // then
        an[NumberNotFoundError.type] should be thrownBy usecases
          .updateNumberFromUser(user.id, newNumberWithNonExistingId)
          .unsafeRunSync()

      }
    }

    "provide with a deleteNumberFromUser method to delete an existing number from an user" which {
      "returns the updated user" in {
        // given
        val user = insertUserInDb(
          NewUser(
            FirstName("enrique"),
            LastName("molina"),
            Set(Mail("enrique@yahoo.com")),
            Set(Number("55555"), Number("66666"))
          )
        )

        val numberWithIdToDelete = user.phoneNumbers.head

        // when
        val result =
          usecases
            .deleteNumberFromUser(user.id, numberWithIdToDelete.id)
            .unsafeRunSync()

        // then
        val expectedUpdatedUser =
          user.copy(phoneNumbers = user.phoneNumbers - numberWithIdToDelete)

        result should be(expectedUpdatedUser)
        findUserInDb(user.id).value should be(expectedUpdatedUser)
      }

      "fails if the user does not exist" in {
        // given
        val nonExistingUserId = UserId(1)
        val numberIdToDelete = NumberId(1)

        // then
        an[UserNotFoundError.type] should be thrownBy usecases
          .deleteNumberFromUser(nonExistingUserId, numberIdToDelete)
          .unsafeRunSync()
      }

      "fails if the number does not exist" in {
        // given
        val user = insertUserInDb(
          NewUser(
            FirstName("enrique"),
            LastName("molina"),
            Set(Mail("enrique@yahoo.com")),
            Set(Number("55555"))
          )
        )

        val numberIdToDelete = NumberId(25)

        // then
        an[NumberNotFoundError.type] should be thrownBy usecases
          .deleteNumberFromUser(user.id, numberIdToDelete)
          .unsafeRunSync()
      }

      "fails if the user only has one number left" in {
        // given
        val user = insertUserInDb(
          NewUser(
            FirstName("enrique"),
            LastName("molina"),
            Set(Mail("enrique@yahoo.com")),
            Set(Number("55555"))
          )
        )

        val numberIdToDelete = user.phoneNumbers.head.id

        // then
        an[NotEnoughNumbersError.type] should be thrownBy usecases
          .deleteNumberFromUser(user.id, numberIdToDelete)
          .unsafeRunSync()
      }
    }
  }

  private def insertUserInDb(newUser: NewUser): User = {
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
      .unsafeRunSync()
  }

  private def findUserInDb(userId: UserId): Option[User] =
    sql"""
          select *
          from users
          where id = $userId
        """.query[User].option.transact(xa).unsafeRunSync()

}
