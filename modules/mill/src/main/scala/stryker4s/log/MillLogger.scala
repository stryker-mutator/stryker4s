package stryker4s.log

import mill.api.daemon.Logger as MillInternalLogger

class MillLogger(millLogger: MillInternalLogger) extends Logger {

  override def log(level: Level, msg: => String): Unit = level match {
    case Level.Debug => if millLogger.debugEnabled then millLogger.debug(msg)
    case Level.Info  => millLogger.info(msg)
    case Level.Warn  => millLogger.warn(msg)
    case Level.Error => millLogger.error(msg)
  }

  override def log(level: Level, msg: => String, e: => Throwable): Unit = {
    log(level, msg)
    log(level, e.toString())
  }

  override protected def colorEnabled: Boolean = mill.api.daemon.loggerColorEnabled(millLogger)
}
