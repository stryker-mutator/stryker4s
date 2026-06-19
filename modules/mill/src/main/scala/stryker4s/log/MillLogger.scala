package stryker4s.log

import mill.api.daemon.Logger as MillInternalLogger

import java.io.{PrintWriter, StringWriter}
import scala.util.Using

class MillLogger(millLogger: MillInternalLogger, env: Map[String, String]) extends Logger {

  override def log(level: Level, msg: => String): Unit = level match {
    case Level.Debug => if millLogger.debugEnabled then millLogger.debug(msg)
    case Level.Info  => millLogger.info(msg)
    case Level.Warn  => millLogger.warn(msg)
    case Level.Error => millLogger.error(msg)
  }

  override def log(level: Level, msg: => String, e: => Throwable): Unit = {
    log(level, msg)
    log(level, stackTraceOf(e))
  }

  private def stackTraceOf(e: Throwable): String =
    val writer = new StringWriter()
    Using.resource(new PrintWriter(writer)): pw =>
      e.printStackTrace(pw)
      writer.toString()

  override protected def colorEnabled: Boolean =
    mill.api.daemon.loggerColorEnabled(millLogger) && !env.contains("NO_COLOR")
}
