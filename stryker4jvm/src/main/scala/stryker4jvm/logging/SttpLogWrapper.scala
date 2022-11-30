package stryker4jvm.logging

import cats.effect.IO
import stryker4jvm.core.logging.{LogLevel, Logger}
import sttp.client3.logging as sttp

/** Wraps a [[stryker4jvm.core.logging.Logger]] to a sttp Logger
  */
class SttpLogWrapper(implicit log: Logger) extends sttp.Logger[IO] {

  override def apply(level: sttp.LogLevel, message: => String): IO[Unit] = IO.delay(log.log(toLevel(level), message))

  override def apply(level: sttp.LogLevel, message: => String, t: Throwable): IO[Unit] =
    IO.delay(log.log(toLevel(level), message, t))

  def toLevel: sttp.LogLevel => LogLevel = {
    case sttp.LogLevel.Trace => LogLevel.Debug
    case sttp.LogLevel.Debug => LogLevel.Debug
    case sttp.LogLevel.Info  => LogLevel.Info
    case sttp.LogLevel.Warn  => LogLevel.Warn
    case sttp.LogLevel.Error => LogLevel.Error
  }
}
