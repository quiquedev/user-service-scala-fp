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
      "return 201 if the user was created" in new TestEnvironment {}
      "return 400 if the user last name is null" in new TestEnvironment {}
      "return 400 if the user last name is empty" in new TestEnvironment {}
      "return 400 if the user last name too big" in new TestEnvironment {}
      "return 400 if the user first name is null" in new TestEnvironment {}
      "return 400 if the user first name is empty" in new TestEnvironment {}
      "return 400 if the user first name is too big" in new TestEnvironment {}
      "return 400 if the email list is null" in new TestEnvironment {}
      "return 400 if the email list is empty" in new TestEnvironment {}
      "return 400 if the email list is too big" in new TestEnvironment {}
      "return 400 if the email list contains invalid emails" in new TestEnvironment {}
      "return 400 if the phone number list is null" in new TestEnvironment {}
      "return 400 if the phone number list is empty" in new TestEnvironment {}
      "return 400 if the phone number list is too big" in new TestEnvironment {}
    }
  }

}
