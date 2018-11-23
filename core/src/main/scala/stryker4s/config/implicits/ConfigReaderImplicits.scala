package stryker4s.config.implicits
import java.nio.file.Path

import better.files.File
import org.apache.logging.log4j.Level
import pureconfig.ConfigReader
import stryker4s.mutants.Exclusions
import stryker4s.run.report.{ConsoleReporter, MutantRunReporter}

trait ConfigReaderImplicits {

  /** Converts a [[java.nio.file.Path]] to a [[better.files.File]] so PureConfig can read it
    *
    */
  private[config] implicit val toFileReader: ConfigReader[File] =
    ConfigReader[Path] map (p => File(p))

  private[config] implicit val logLevelReader: ConfigReader[Level] =
    ConfigReader[String] map (level => Level.valueOf(level))

  private[config] implicit val toReporterList: ConfigReader[MutantRunReporter] =
    ConfigReader[String] map {
      case MutantRunReporter.`consoleReporter` => new ConsoleReporter
    }

  private[config] implicit val exclusions: ConfigReader[Exclusions] =
    ConfigReader[List[String]] map (exclusions => Exclusions(exclusions.toSet))
}
