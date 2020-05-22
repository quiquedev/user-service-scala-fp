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
