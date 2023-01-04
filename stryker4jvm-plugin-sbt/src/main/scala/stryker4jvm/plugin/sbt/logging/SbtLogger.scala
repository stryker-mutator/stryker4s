package stryker4jvm.plugin.sbt.logging

import sbt.{Level as SbtLevel, Logger as SbtInternalLogger}
import stryker4jvm.core.logging.{LogLevel, Logger}
import stryker4jvm.logging.FansiLogger

class FansiSbtLogger(sbtLogger: SbtInternalLogger) extends FansiLogger(new SbtLogger(sbtLogger)) {}

private class SbtLogger(sbtLogger: SbtInternalLogger) extends Logger {

  def log(level: LogLevel, msg: String): Unit = sbtLogger.log(toSbtLevel(level), msg)

  def log(level: LogLevel, msg: String, e: Throwable): Unit = {
    sbtLogger.log(toSbtLevel(level), msg)
    sbtLogger.trace(e)
  }

  def log(level: LogLevel, e: Throwable): Unit = sbtLogger.trace(e)

  private def toSbtLevel(level: LogLevel): SbtLevel.Value = level match {
    case LogLevel.Debug => SbtLevel.Debug
    case LogLevel.Info  => SbtLevel.Info
    case LogLevel.Warn  => SbtLevel.Warn
    case LogLevel.Error => SbtLevel.Error
  }

  override def determineColorEnabled(): Boolean =
    sbt.internal.util.Terminal.console.isColorEnabled && !sys.env.contains("NO_COLOR")
}
