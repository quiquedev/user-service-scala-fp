package info.quiquedev.userservice

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import java.time.Clock

object Main extends IOApp {
  implicit val clock = Clock.systemUTC

  def run(args: List[String]) =
    UserServiceServer.stream[IO].compile.drain.as(ExitCode.Success)
}