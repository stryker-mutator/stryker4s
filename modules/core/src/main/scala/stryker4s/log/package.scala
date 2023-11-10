package stryker4s

import fansi.Str

import stryker4s.log.Level.*

package object log {

  implicit final class LogExtensions(val log: Logger) extends AnyVal {
    final def debug(msg: => Str): Unit = logImpl(Debug, msg)
    final def debug(msg: => Str, e: Throwable): Unit = logImpl(Debug, msg, e)

    final def info(msg: => Str): Unit = logImpl(Info, msg)
    final def info(msg: => Str, e: Throwable): Unit = logImpl(Info, msg, e)

    final def warn(msg: => Str): Unit = logImpl(Warn, msg)
    final def warn(msg: => Str, e: Throwable): Unit = logImpl(Warn, msg, e)

    final def error(msg: => Str): Unit = logImpl(Error, msg)
    final def error(msg: => Str, e: Throwable): Unit = logImpl(Error, msg, e)

    final private def logImpl(level: Level, msg: => Str): Unit = log.log(level, () => processMsgStr(msg))
    final private def logImpl(level: Level, msg: => Str, e: Throwable): Unit =
      log.log(level, () => processMsgStr(msg), e)

    /** Process a colored fansi.Str to a String, or plain text if colors are disabled
      */
    private def processMsgStr(msg: fansi.Str): String =
      if (log.colorEnabled) msg.render else msg.plainText

  }
}
