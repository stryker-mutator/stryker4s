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
    info(s"Set logging level to $level")
  }
}
