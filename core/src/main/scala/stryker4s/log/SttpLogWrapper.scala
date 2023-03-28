package stryker4s.log

import cats.effect.IO
import sttp.client4.logging as sttp

/** Wraps a [[stryker4s.log.Logger]] to a sttp Logger
  */
class SttpLogWrapper(implicit log: Logger) extends sttp.Logger[IO] {

  override def apply(level: sttp.LogLevel, message: => String): IO[Unit] = IO.delay(log.log(toLevel(level), message))

  override def apply(level: sttp.LogLevel, message: => String, t: Throwable): IO[Unit] =
    IO.delay(log.log(toLevel(level), message, t))

  def toLevel: sttp.LogLevel => Level = {
    case sttp.LogLevel.Trace => Debug
    case sttp.LogLevel.Debug => Debug
    case sttp.LogLevel.Info  => Info
    case sttp.LogLevel.Warn  => Warn
    case sttp.LogLevel.Error => Error
  }
}
