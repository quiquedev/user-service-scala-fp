package info.quiquedev.userservice

import cats.effect.Sync
import org.slf4j.LoggerFactory
import scala.util.Try
import cats.implicits._

trait Logger[F[_]] {
  def error(msg: String, t: Throwable): F[Unit]
}

object Logger {
  def apply[F[_]](implicit ev: Logger[F]): Logger[F] = ev

  def impl[F[_]: Sync](name: String): Logger[F] = new Logger[F] {
    private val logger = LoggerFactory.getLogger(name)
    override def error(msg: String, t: Throwable): F[Unit] =
      Try(logger.error(msg, t)) match {
        case _ => ().pure[F]
      }
  }
}
