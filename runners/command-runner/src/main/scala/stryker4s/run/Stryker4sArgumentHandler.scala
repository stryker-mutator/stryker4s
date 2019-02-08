package stryker4s.run
import grizzled.slf4j.Logging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator

object Stryker4sArgumentHandler extends Logging {
  private lazy val logLevels: Map[String, Level] =
    Level.values().map(level => (level.toString.toLowerCase, level)).toMap

  def parseArgs(args: Array[String]): Unit = {
    // Collect and handle log level argument
    args
      .filter(_.head == '-')
      .map(_.drop(1))
      .collectFirst { case arg if logLevels.contains(arg) => setLogLevel(logLevels(arg)) }
      .getOrElse(setLogLevel(Level.INFO))
  }

  private def setLogLevel(level: Level): Unit = {
    Configurator.setRootLevel(level)
    val message = s"Set logging level to $level"
    level match {
      case Level.ERROR => error(message)
      case Level.WARN => warn(message)
      case Level.INFO | Level.ALL => info(message)
      case Level.DEBUG => debug(message)
      case Level.TRACE => trace(message)
      case _ => // ignore on OFF and FATAL
    }
  }
}
