package stryker4s.log

import org.apache.maven.plugin.logging.Log
import org.apache.maven.shared.utils.logging.MessageUtils

class MavenMojoLogger(mavenLogger: Log) extends Logger {

  override def log(level: Level, msg: => String): Unit = doLog(level)(
    mavenLogger.debug(msg),
    mavenLogger.info(msg),
    mavenLogger.warn(msg),
    mavenLogger.error(msg)
  )

  override def log(level: Level, msg: => String, e: => Throwable): Unit = doLog(level)(
    mavenLogger.debug(msg, e),
    mavenLogger.info(msg, e),
    mavenLogger.warn(msg, e),
    mavenLogger.error(msg, e)
  )

  private def doLog(level: Level)(
      onDebug: => Unit,
      onInfo: => Unit,
      onWarn: => Unit,
      onError: => Unit
  ): Unit = level match {
    case Level.Debug => if (mavenLogger.isDebugEnabled()) onDebug
    case Level.Info  => if (mavenLogger.isInfoEnabled()) onInfo
    case Level.Warn  => if (mavenLogger.isWarnEnabled()) onWarn
    case Level.Error => if (mavenLogger.isErrorEnabled()) onError
  }

  override val colorEnabled = MessageUtils.isColorEnabled() && !sys.env.contains("NO_COLOR")
}
