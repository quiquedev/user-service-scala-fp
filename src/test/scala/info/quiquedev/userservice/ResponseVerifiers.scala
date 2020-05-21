package info.quiquedev.userservice

import cats.effect.IO
import org.http4s._
import org.scalatest.Suite

trait ResponseVerifiers { self: Suite =>

  private def verifyResponse[A](
      actual: IO[Option[Response[IO]]],
      expectedStatusCode: Int,
      expectedBody: A
  )(extractBody: Response[IO] => A): Unit = {
    val actualResp = actual.unsafeRunSync.getOrElse(fail("missing response"))

    assert(actualResp.status.code == expectedStatusCode)

    assert(extractBody(actualResp) == expectedBody)
    ()
  }

  def verifyJsonResponse[A](
      actual: IO[Option[Response[IO]]],
      expectedStatusCode: Int,
      expectedBody: A
  )(
      implicit ev: EntityDecoder[IO, A]
  ): Unit =
    verifyResponse(actual, expectedStatusCode, expectedBody)(
      _.as[A].unsafeRunSync()
    )

  def verifyTextResponse[A](
      actual: IO[Option[Response[IO]]],
      expectedStatusCode: Int,
      errorMsg: String
  ): Unit =
    verifyResponse(actual, expectedStatusCode, errorMsg)(
      _.bodyAsText.compile.toVector.unsafeRunSync().head
    )

  def verifyEmptyResponse[A](
      actual: IO[Option[Response[IO]]],
      expectedStatusCode: Int
  ): Unit = {
    val actualResp = actual.unsafeRunSync.getOrElse(fail("missing response"))

    assert(actualResp.status.code == expectedStatusCode)
    ()
  }
}
