package stryker4s.run

import ch.qos.logback.classic.{Level, Logger}
import grizzled.slf4j.Logging
import org.slf4j.{LoggerFactory, Logger => slf4jLogger}
import stryker4s.Stryker4s
import stryker4s.config.{CommandRunner, Config, ConfigReader}
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{FileCollector, MutantFinder, MutantMatcher}
import stryker4s.run.process.{Command, ProcessRunner}
import stryker4s.run.report.Reporter

object Stryker4sRunner extends App with Logging {

  implicit val config: Config = ConfigReader.readConfig()
  setLoggingLevel(config.logLevel)

  val stryker4s = new Stryker4s(
    new FileCollector,
    new Mutator(new MutantFinder(new MutantMatcher, new MutantRegistry),
                new StatementTransformer,
                new MatchBuilder),
    resolveRunner(),
    new Reporter()
  )

  stryker4s.run()

  /**
    * Sets the logging level to one of the following levels:
    * OFF, ERROR, WARN, INFO, DEBUG, TRACE, ALL
    *
    * @param level the logging level to use
    */
  private def setLoggingLevel(level: Level): Unit = {
    LoggerFactory.getLogger(slf4jLogger.ROOT_LOGGER_NAME).asInstanceOf[Logger].setLevel(level)
    info(s"Setting logging level to $level.")
  }

  private def resolveRunner()(implicit config: Config): MutantRunner = {
    config.testRunner match {
      case CommandRunner(command, args) =>
        new ProcessMutantRunner(Command(command, args), ProcessRunner.resolveRunner())
    }
  }
}
