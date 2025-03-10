package stryker4s.log

import cats.effect.IO
import sttp.client4.logging as sttp

/** Wraps a `stryker4s.log.Logger` to a sttp Logger
  */
class SttpLogWrapper(implicit log: Logger) extends sttp.Logger[IO] {

  override def apply(
      level: sttp.LogLevel,
      message: => String,
      exception: Option[Throwable],
      context: Map[String, Any]
  ): IO[Unit] = exception match {
    case None    => IO.delay(log.log(toLevel(level), message))
    case Some(t) => IO.delay(log.log(toLevel(level), message, t))
  }

  def toLevel: sttp.LogLevel => Level = {
    case sttp.LogLevel.Trace => Level.Debug
    case sttp.LogLevel.Debug => Level.Debug
    case sttp.LogLevel.Info  => Level.Info
    case sttp.LogLevel.Warn  => Level.Warn
    case sttp.LogLevel.Error => Level.Error
  }
}
