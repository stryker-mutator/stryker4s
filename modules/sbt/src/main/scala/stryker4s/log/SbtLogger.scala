package stryker4s.log

import sbt.{Level as SbtLevel, Logger as SbtInternalLogger}

class SbtLogger(sbtLogger: SbtInternalLogger) extends Logger {

  override def log(level: Level, msg: => String): Unit = sbtLogger.log(toSbtLevel(level), msg)

  override def log(level: Level, msg: => String, e: => Throwable): Unit = {
    sbtLogger.log(toSbtLevel(level), msg)
    sbtLogger.trace(e)
  }

  private def toSbtLevel(level: Level): SbtLevel.Value = level match {
    case Level.Debug => SbtLevel.Debug
    case Level.Info  => SbtLevel.Info
    case Level.Warn  => SbtLevel.Warn
    case Level.Error => SbtLevel.Error
  }

  override protected val colorEnabled: Boolean =
    sbt.internal.util.Terminal.console.isColorEnabled && !sys.env.contains("NO_COLOR")
}
