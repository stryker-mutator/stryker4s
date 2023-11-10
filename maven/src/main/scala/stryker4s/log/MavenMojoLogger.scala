package stryker4s.log

import org.apache.maven.plugin.logging.Log
import org.apache.maven.shared.utils.logging.MessageUtils

import java.util.function.Supplier

class MavenMojoLogger(mavenLogger: Log) extends Logger {

  override def log(level: Level, msg: Supplier[String]): Unit = doLog(level)(
    mavenLogger.debug(msg.get),
    mavenLogger.info(msg.get),
    mavenLogger.warn(msg.get),
    mavenLogger.error(msg.get)
  )

  override def log(level: Level, msg: Supplier[String], e: Throwable): Unit = doLog(level)(
    mavenLogger.debug(msg.get, e),
    mavenLogger.info(msg.get, e),
    mavenLogger.warn(msg.get, e),
    mavenLogger.error(msg.get, e)
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
