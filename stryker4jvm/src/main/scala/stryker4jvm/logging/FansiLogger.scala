package stryker4jvm.logging
import fansi.Str
import stryker4jvm.core.logging.{LogLevel, Logger}

class FansiLogger(val coreLogger: Logger) {

  final def log(level: LogLevel, msg: => String): Unit = coreLogger.log(level, msg)
  final def log(level: LogLevel, msg: => String, e: => Throwable): Unit = coreLogger.log(level, msg, e)
  final def log(level: LogLevel, throwable: Throwable): Unit = coreLogger.log(level, throwable)

  final def debug(msg: => Str): Unit = logImpl(LogLevel.Debug, msg)
  final def debug(msg: => Str, e: Throwable): Unit = logImpl(LogLevel.Debug, msg, e)
  final def debug(e: Throwable): Unit = log(LogLevel.Debug, e)

  final def info(msg: => Str): Unit = logImpl(LogLevel.Info, msg)
  final def info(msg: => Str, e: Throwable): Unit = logImpl(LogLevel.Info, msg, e)
  final def info(e: Throwable): Unit = log(LogLevel.Info, e)

  final def warn(msg: => Str): Unit = logImpl(LogLevel.Warn, msg)
  final def warn(msg: => Str, e: Throwable): Unit = logImpl(LogLevel.Warn, msg, e)
  final def warn(e: Throwable): Unit = log(LogLevel.Warn, e)

  final def error(msg: => Str): Unit = logImpl(LogLevel.Error, msg)
  final def error(msg: => Str, e: Throwable): Unit = logImpl(LogLevel.Error, msg, e)
  final def error(e: Throwable): Unit = log(LogLevel.Error, e)

  final private def logImpl(level: LogLevel, msg: => Str): Unit = log(level, processMsgStr(msg))
  final private def logImpl(level: LogLevel, msg: => Str, e: => Throwable): Unit = log(level, processMsgStr(msg), e)

  /** Process a colored fansi.Str to a String, or plain text if colors are disabled */
  @inline private def processMsgStr(msg: fansi.Str): String =
    if (coreLogger.isColorEnabled) msg.render else msg.plainText
}
