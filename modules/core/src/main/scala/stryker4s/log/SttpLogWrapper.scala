package stryker4s.log

import cats.effect.IO
import sttp.client3.logging as sttp

/** Wraps a `stryker4s.log.Logger` to a sttp Logger
  */
class SttpLogWrapper(implicit log: Logger) extends sttp.Logger[IO] {

  override def apply(level: sttp.LogLevel, message: => String): IO[Unit] =
    IO.delay(log.log(toLevel(level), () => message))

  override def apply(level: sttp.LogLevel, message: => String, t: Throwable): IO[Unit] =
    IO.delay(log.log(toLevel(level), () => message, t))

  def toLevel: sttp.LogLevel => Level = {
    case sttp.LogLevel.Trace => Level.Debug
    case sttp.LogLevel.Debug => Level.Debug
    case sttp.LogLevel.Info  => Level.Info
    case sttp.LogLevel.Warn  => Level.Warn
    case sttp.LogLevel.Error => Level.Error
  }
}
