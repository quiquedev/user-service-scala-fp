package info.quiquedev.userservice

import java.time.Clock

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._

object Main extends IOApp {
  implicit val clock = Clock.systemUTC

  def run(args: List[String]) =
    ConnectaServer.stream[IO].compile.drain.as(ExitCode.Success)
}
