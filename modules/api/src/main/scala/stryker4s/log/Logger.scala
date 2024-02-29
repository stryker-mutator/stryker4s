package stryker4s.log

import stryker4s.log.Level.*
import fansi.Str

trait Logger {

  // These two methods are all that's needed to implement a logger
  def log(level: Level, msg: => String): Unit
  def log(level: Level, msg: => String, t: => Throwable): Unit

  // These provide syntactic sugar for logging

  final def debug(msg: => Str): Unit = doLog(Debug, msg)
  final def debug(msg: => Str, e: Throwable): Unit = doLog(Debug, msg, e)

  final def info(msg: => Str): Unit = doLog(Info, msg)
  final def info(msg: => Str, e: Throwable): Unit = doLog(Info, msg, e)

  final def warn(msg: => Str): Unit = doLog(Warn, msg)
  final def warn(msg: => Str, e: Throwable): Unit = doLog(Warn, msg, e)

  final def error(msg: => Str): Unit = doLog(Error, msg)
  final def error(msg: => Str, e: Throwable): Unit = doLog(Error, msg, e)

  final private def doLog(level: Level, msg: => Str): Unit = log(level, processMsgStr(msg))
  final private def doLog(level: Level, msg: => Str, e: Throwable): Unit =
    log(level, processMsgStr(msg), e)

  /** Process a colored fansi.Str to a String, or plain text if colors are disabled
    */
  private def processMsgStr(msg: fansi.Str): String =
    if (colorEnabled) msg.render else msg.plainText

  /** Whether colors are enabled in the log. Loggers can override this, a build tool might provide a flag to
    * disable/enable colors
    */
  protected def colorEnabled: Boolean = {

    // Explicitly disable color https://no-color.org/
    val notNoColor = !sys.env.contains("NO_COLOR")
    // If there is a TERM on Linux (or Windows Git Bash), assume we support color
    val unixEnabled = sys.env.contains("TERM")
    // On Windows there's no easy way. But if we're in Windows Terminal or ConEmu,
    // we can assume we support color
    val windowsEnabled = sys.env.contains("WT_SESSION") || (sys.env.get("ConEmuANSI").contains("ON"))

    notNoColor && (unixEnabled || windowsEnabled)
  }
}
