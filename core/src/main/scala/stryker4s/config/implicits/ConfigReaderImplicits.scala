package stryker4s.config.implicits

import java.nio.file.Path

import better.files.File
import grizzled.slf4j.Logging
import org.apache.logging.log4j.Level
import pureconfig.ConfigReader
import stryker4s.extension.exception.InvalidExclusionsException
import stryker4s.extension.mutationtype.Mutation
import stryker4s.run.report.{ConsoleReporter, HtmlReporter, MutantRunReporter}

trait ConfigReaderImplicits extends Logging {

  /** Converts a [[java.nio.file.Path]] to a [[better.files.File]] so PureConfig can read it
    *
    */
  private[config] implicit val toFileReader: ConfigReader[File] =
    ConfigReader[Path] map (p => File(p))

  private[config] implicit val logLevelReader: ConfigReader[Level] =
    ConfigReader[String] map (level => Level.valueOf(level))

  private[config] implicit val toReporterList: ConfigReader[MutantRunReporter] =
    ConfigReader[String] map {
      case MutantRunReporter.consoleReporter => new ConsoleReporter
      case MutantRunReporter.htmlReporter => new HtmlReporter
    }

  private[config] implicit val exclusions: ConfigReader[Set[String]] =
    ConfigReader[List[String]].map(errorOnInvalidExclusions).map(_.toSet)

  private def errorOnInvalidExclusions(exclusions: List[String]): List[String] = {
    val (valid, invalid) = exclusions.partition(Mutation.mutations.contains)
    if (invalid.nonEmpty) throw InvalidExclusionsException(invalid)

    valid
  }
}
