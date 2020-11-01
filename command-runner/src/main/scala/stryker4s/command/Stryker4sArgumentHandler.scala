package stryker4s.command

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import stryker4s.log.{Logger, Slf4jLogger}

object Stryker4sArgumentHandler {
  private lazy val logLevels: Map[String, Level] = Level
    .values()
    .map(level => (level.toString.toLowerCase, level))
    .toMap

  /** Handle args will parse the giving arguments to the jvm.
    * For now we search for a log level and handle those appropriately.
    */
  def handleArgs(args: Seq[String]): Logger = {
    // Collect and handle log level argument
    val logLevel = args
      .filter(_.startsWith("--"))
      .map(_.drop(2))
      .map(_.toLowerCase)
      .find(logLevels.contains)
      .map(logLevels(_))
      .getOrElse(Level.INFO)

    val logger = new Slf4jLogger()
    Configurator.setRootLevel(logLevel)

    logger.info(s"Set logging level to $logLevel")
    logger
  }

}
