package info.quiquedev.userservice

import org.http4s.HttpRoutes
import org.http4s._
import java.time.Instant
import org.scalatest.wordspec.AnyWordSpec
import cats.effect.IO
import org.mockito.MockitoSugar
import org.http4s.HttpRoutes
import org.mockito.ArgumentMatchersSugar
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.http4s.Uri.uri
import org.scalatest.matchers.should.Matchers
import cats.implicits._
import info.quiquedev.userservice.UserUsecases
import junit.framework.Test
import info.quiquedev.userservice.Dto.NewUserDto
import info.quiquedev.userservice.Domain.FirstName
import org.mockito.ArgumentMatchers.{eq => equ}
import info.quiquedev.userservice.Dto.FirstNameDto
import info.quiquedev.userservice.Dto.LastNameDto
import info.quiquedev.userservice.Dto.MailDto
import info.quiquedev.userservice.Dto.NumberDto
import info.quiquedev.userservice.Domain.UserId
import Domain._
import io.circe.parser._
import org.scalatest.EitherValues
import org.http4s.circe._
import io.circe.syntax._
import io.circe._
import io.circe.generic.semiauto._

class RoutesSpec
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with ArgumentMatchersSugar
    with ResponseVerifiers {

  trait TestEnvironment {
    implicit val usecases: UserUsecases[IO] = mock[UserUsecases[IO]]
    val routes = Routes.all[IO]
  }

  "/users" should {
    "support POST request to create an user" which {
      "return 201 if the user was created" in new TestEnvironment {
        // given
        val requestBody = """
        {
          "firstName": "enrique",
          "lastName": "molina",
          "emails": ["emolina@gmail.com"],
          "phoneNumbers": ["12345"]
        }
        """

        val newUser = NewUser(
          FirstName("enrique"),
          LastName("molina"),
          Set(Mail("emolina@gmail.com")),
          Set(Number("12345"))
        )

        val user = User(
          UserId(1),
          FirstName("enrique"),
          LastName("molina"),
          List(Email(EmailId(1), Mail("emolina@gmail.com"))),
          List(PhoneNumber(EmailId(1), Number("12345")))
        )

        when(usecases.createUser(newUser)) thenReturn user.pure[IO]

        // when
        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        val responseBody = """
        {
          "id": 1,
          "lastName": "molina",
          "firstName": "enrique",
          "emails": [{"id": 1, "mail": "emolina@gmail.com"}],
          "phoneNumbers": [{"id": 1, "number": "12345"}]
        }
        """

        verifyJsonResponse(response, 201, parse(responseBody).getOrElse(fail()))
      }

      "return 400 if the user last name is not present" in new TestEnvironment {
        // given
        val requestBody = """
        {
          "firstName": "enrique",
          "emails": ["emolina@gmail.com"],
          "phoneNumbers": ["12345"]
        }
        """

        // when
        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(
          response,
          400,
          "lastName must be present and not null"
        )
      }

      "return 400 if the user last name is null" in new TestEnvironment {
        // given
        val requestBody = """
        {
          "lastName": null,
          "firstName": "enrique",
          "emails": ["emolina@gmail.com"],
          "phoneNumbers": ["12345"]
        }
        """

        // when
        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(
          response,
          400,
          "lastName must be present and not null"
        )
      }

      "return 400 if the user last name is empty" in new TestEnvironment {
        // given
        val requestBody = """
        {
          "lastName": " ",
          "firstName": "enrique",
          "emails": ["emolina@gmail.com"],
          "phoneNumbers": ["12345"]
        }
        """

        // when
        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(
          response,
          400,
          "lastName cannot be empty"
        )

      }

      "return 400 if the user last name is too long" in new TestEnvironment {
        // given
        val requestBody = s"""
        {
          "lastName": "${"a" * 501}",
          "firstName": "enrique",
          "emails": ["emolina@gmail.com"],
          "phoneNumbers": ["12345"]
        }
        """

        // when
        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(
          response,
          400,
          "lastName can have a max length of 500"
        )
      }

      "return 400 if the user first name is not present" in new TestEnvironment {
        // given
        val requestBody = """
        {
          "lastName": "molina",
          "emails": ["emolina@gmail.com"],
          "phoneNumbers": ["12345"]
        }
        """

        // when
        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(
          response,
          400,
          "firstName must be present and not null"
        )
      }

      "return 400 if the user first name is null" in new TestEnvironment {
        // given
        val requestBody = """
        {
          "lastName": "molina",
          "firstName": null,
          "emails": ["emolina@gmail.com"],
          "phoneNumbers": ["12345"]
        }
        """

        // when
        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(
          response,
          400,
          "firstName must be present and not null"
        )
      }

      "return 400 if the user first name is empty" in new TestEnvironment {
        // given
        val requestBody = """
        {
          "firstName": " ",
          "lastName": "molina",
          "emails": ["emolina@gmail.com"],
          "phoneNumbers": ["12345"]
        }
        """

        // when
        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(
          response,
          400,
          "firstName cannot be empty"
        )

      }

      "return 400 if the user first name is too long" in new TestEnvironment {
        // given
        val requestBody = s"""
        {
          "firstName": "${"a" * 501}",
          "lastName": "moline",
          "emails": ["emolina@gmail.com"],
          "phoneNumbers": ["12345"]
        }
        """

        // when
        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(
          response,
          400,
          "firstName can have a max length of 500"
        )
      }

      "return 400 if the email list is not present" in new TestEnvironment {
        // given
        val requestBody = """
        {
          "firstName": "enrique",
          "lastName": "molina",
          "phoneNumbers": ["12345"]
        }
        """

        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(response, 400, "emails must be present and not null")
      }

      "return 400 if the email list is null" in new TestEnvironment {
        // given
        val requestBody = """
        {
          "firstName": "enrique",
          "lastName": "molina",
          "emails": null,
          "phoneNumbers": ["12345"]
        }
        """

        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(response, 400, "emails must be present and not null")
      }

      "return 400 if the email list is empty" in new TestEnvironment {
        // given
        val requestBody = """
        {
          "firstName": "enrique",
          "lastName": "molina",
          "emails": [],
          "phoneNumbers": ["12345"]
        }
        """

        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(response, 400, "emails cannot be empty")
      }

      "return 400 if the email list is too big" in new TestEnvironment {
        // given
        val requestBody =
          """
        {
          "firstName": "enrique",
          "lastName": "molina",
          "emails": ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"],
          "phoneNumbers": ["12345"]
        }
        """

        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(response, 400, "emails can have a max size of 10")
      }

      "return 400 if the email list contains too long emails" in new TestEnvironment {
        // given
        val longEmail = "a" * 1000

        val requestBody =
          s"""
          {
            "firstName": "enrique",
            "lastName": "molina",
            "emails": ["$longEmail"],
            "phoneNumbers": ["12345"]
          }
          """

        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(
          response,
          400,

          s"mail '$longEmail' is too long (max length 500)"
        )
      }

      "return 400 if the phone number list is not present" in new TestEnvironment {
        // given
        val requestBody = """
        {
          "firstName": "enrique",
          "lastName": "molina",
          "emails": ["1"]
        }
        """

        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(response, 400, "phoneNumbers must be present and not null")
      }

      "return 400 if the phone number list is null" in new TestEnvironment {
        // given
        val requestBody = """
        {
          "firstName": "enrique",
          "lastName": "molina",
          "emails": ["1"],
          "phoneNumbers": null
        }
        """

        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(response, 400, "phoneNumbers must be present and not null")
      }

      "return 400 if the phone number list is empty" in new TestEnvironment {
        // given
        val requestBody = """
        {
          "firstName": "enrique",
          "lastName": "molina",
          "emails": ["1"],
          "phoneNumbers": []
        }
        """

        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(response, 400, "phoneNumbers cannot be empty")
      }

      "return 400 if the phone number list is too big" in new TestEnvironment {
        // given
        val requestBody =
          """
        {
          "firstName": "enrique",
          "lastName": "molina",
          "emails": ["1"],
          "phoneNumbers": ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"]
        }
        """

        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(response, 400, "phoneNumbers can have a max size of 10")
      }

      "return 400 if the phone number list contains too long emails" in new TestEnvironment {
        // given
        val longNumber = "1" * 1000

        val requestBody =
          s"""
          {
            "firstName": "enrique",
            "lastName": "molina",
            "emails": ["1"],
            "phoneNumbers": ["$longNumber"]
          }
          """

        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(
          response,
          400,
          s"number '$longNumber' is too long (max length 500)"
        )
      }

      "return 400 if more than one validation fails" in new TestEnvironment {
        // given
        val requestBody =
          s"""
          {
            "emails": ["1"],
            "phoneNumbers": ["1"]
          }
          """

        val response =
          routes
            .run(
              Request[IO](
                method = Method.POST,
                uri = uri("/users")
              ).withEntity(parse(requestBody).getOrElse(fail()))
            )
            .value

        // then
        verifyTextResponse(
          response,
          400,
          s"lastName must be present and not null,firstName must be present and not null"
        )
      }
 
    }

  }
}
