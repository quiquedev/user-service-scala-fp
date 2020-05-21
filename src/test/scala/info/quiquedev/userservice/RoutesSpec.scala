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
          List(Mail("emolina@gmail.com")),
          List(Number("12345"))
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
          "firstName": "enrique",
          "lastName": "molina",
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
          "last name must be present and not null"
        )
      }

      "return 400 if the user last name is null" in new TestEnvironment {
        // given
        val requestBody = """
        {
          "firstName": "enrique",
          "lastName": null,
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
          "last name must be present and not null"
        )
      }
      "return 400 if the user last name is empty" in new TestEnvironment {}
      "return 400 if the user last name too big" in new TestEnvironment {}
      "return 400 if the user first name is not present" in new TestEnvironment {}
      "return 400 if the user first name is null" in new TestEnvironment {}
      "return 400 if the user first name is empty" in new TestEnvironment {}
      "return 400 if the user first name is too big" in new TestEnvironment {}
      "return 400 if the email list is not present" in new TestEnvironment {}
      "return 400 if the email list is null" in new TestEnvironment {}
      "return 400 if the email list is empty" in new TestEnvironment {}
      "return 400 if the email list is too big" in new TestEnvironment {}
      "return 400 if the email list contains invalid emails" in new TestEnvironment {}
      "return 400 if the phone number list is not present" in new TestEnvironment {}
      "return 400 if the phone number list is null" in new TestEnvironment {}
      "return 400 if the phone number list is empty" in new TestEnvironment {}
      "return 400 if the phone number list is too big" in new TestEnvironment {}
    }
  }
}
