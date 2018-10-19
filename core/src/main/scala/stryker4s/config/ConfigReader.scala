package stryker4s.config

import java.io.FileNotFoundException
import java.nio.file.Path

import better.files.File
import grizzled.slf4j.Logging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import pureconfig.error.{CannotReadFile, ConfigReaderException, ConfigReaderFailures}
import pureconfig.{Derivation, ConfigReader => PConfigReader}
import stryker4s.model.Exclusions
import stryker4s.run.report.{ConsoleReporter, MutantRunReporter}

object ConfigReader extends Logging {

  /** Converts a [[java.nio.file.Path]] to a [[better.files.File]] so PureConfig can read it
    *
    */
  private[this] implicit val toFileReader: PConfigReader[File] =
    PConfigReader[Path].map(p => File(p))
  private[this] implicit val logLevelReader: PConfigReader[Level] = PConfigReader[String] map (
      level => Level.valueOf(level))
  private[this] implicit val toReporterList: PConfigReader[List[MutantRunReporter]] =
    PConfigReader[List[String]].map(_.map {
      case MutantRunReporter.`consoleReporter` => new ConsoleReporter
    })

  implicit val exclusions: PConfigReader[Exclusions] =
    PConfigReader[List[String]].map(strings => Exclusions.apply(strings:_*))

  /** Read config from stryker4s.conf. Or use the default Config if no config file is found.
    */
  def readConfig(confFile: File = File.currentWorkingDirectory / "stryker4s.conf"): Config =
    pureconfig.loadConfig[Config](confFile.path, namespace = "stryker4s") match {
      case Left(failures) => tryRecoverFromFailures(failures)
      case Right(config) =>
        setLoggingLevel(config.logLevel)
        info("Using stryker4s.conf in the current working directory")

        config
    }

  private def tryRecoverFromFailures(failures: ConfigReaderFailures): Config = failures match {
    case ConfigReaderFailures(CannotReadFile(fileName, Some(_: FileNotFoundException)), _) =>
      val defaultConf = Config()
      setLoggingLevel(defaultConf.logLevel)

      warn(s"Could not find config file $fileName")
      warn("Using default config instead...")
      info("Config used: " + defaultConf.toHoconString)

      defaultConf
    case _ =>
      error("Failures in reading config: ")
      error(failures.toList.map(_.description).mkString(System.lineSeparator))
      throw ConfigReaderException(failures)
  }

  /**
    * Sets the logging level to one of the following levels:
    * OFF, ERROR, WARN, INFO, DEBUG, TRACE, ALL
    *
    * @param level the logging level to use
    */
  private def setLoggingLevel(level: Level): Unit = {
    Configurator.setRootLevel(level)
    info(s"Set logging level to $level.")
  }
}
