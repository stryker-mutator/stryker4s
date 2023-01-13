package stryker4jvm.command

import cats.effect.IO
import org.slf4j.event.Level
import stryker4jvm.command.logging.FansiSlf4jLogger
import stryker4jvm.logging.FansiLogger

object Stryker4jvmArgumentHandler {
  private lazy val logLevels: Map[String, Level] = Level
    .values()
    .map(level => (level.toString.toLowerCase, level))
    .toMap

  /** Handle args will parse the giving arguments to the jvm. For now we search for a log level and handle those
    * appropriately.
    */
  def handleArgs(args: Seq[String]): String = {
    // Collect and handle log level argument
    val logLevel = args
      .filter(_.startsWith("--"))
      .map(_.drop(2))
      .map(_.toLowerCase)
      .flatMap(logLevels.get(_))
      .headOption
      .getOrElse(Level.INFO)

    sys.props(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY) = logLevel.toString();

    s"Set logging level to $logLevel"
  }

  def configureLogger(args: Seq[String]): IO[FansiLogger] =
    for {
      logString <- IO(handleArgs(args))
      logger <- IO(new FansiSlf4jLogger())
      _ <- IO(logger.info(logString))
    } yield logger

}
