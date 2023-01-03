package stryker4s.log

import org.apache.maven.plugin.logging.Log
import org.apache.maven.shared.utils.logging.MessageUtils
import stryker4jvm.core.logging.LogLevel
import stryker4jvm.logging.CoreLogWrapper

class MavenMojoLogger(mavenLogger: Log) extends CoreLogWrapper(new MavenCoreLogger(mavenLogger)) {}

private class MavenCoreLogger(val mavenLogger: Log) extends stryker4jvm.core.logging.Logger {
  override def log(level: LogLevel, msg: String): Unit = doLog(level)(
    mavenLogger.debug(msg),
    mavenLogger.info(msg),
    mavenLogger.warn(msg),
    mavenLogger.error(msg)
  )

  override def log(level: LogLevel, msg: String, e: Throwable): Unit = doLog(level)(
    mavenLogger.debug(msg, e),
    mavenLogger.info(msg, e),
    mavenLogger.warn(msg, e),
    mavenLogger.error(msg, e)
  )

  override def log(level: LogLevel, e: Throwable): Unit = doLog(level)(
    mavenLogger.debug(e),
    mavenLogger.info(e),
    mavenLogger.warn(e),
    mavenLogger.error(e)
  )

  private def doLog(level: LogLevel)(
      onDebug: => Unit,
      onInfo: => Unit,
      onWarn: => Unit,
      onError: => Unit
  ): Unit = level match {
    case LogLevel.Debug => if (mavenLogger.isDebugEnabled) onDebug
    case LogLevel.Info  => if (mavenLogger.isInfoEnabled) onInfo
    case LogLevel.Warn  => if (mavenLogger.isWarnEnabled) onWarn
    case LogLevel.Error => if (mavenLogger.isErrorEnabled) onError
  }

  override protected def determineColorEnabled(): Boolean = MessageUtils.isColorEnabled && !sys.env.contains("NO_COLOR")
}
