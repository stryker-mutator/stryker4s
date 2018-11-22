package stryker4s.config.implicits
import java.nio.file.Path

import better.files.File
import grizzled.slf4j.Logging
import org.apache.logging.log4j.Level
import pureconfig.ConfigReader
import pureconfig.error.ConfigReaderException
import stryker4s.extensions.mutationtypes.Mutation
import stryker4s.mutants.Exclusions
import stryker4s.run.report.{ConsoleReporter, MutantRunReporter}

trait ConfigReaderImplicits extends Logging {

  /** Converts a [[java.nio.file.Path]] to a [[better.files.File]] so PureConfig can read it
    *
    */
  private[config] implicit val toFileReader: ConfigReader[File] =
    ConfigReader[Path].map(p => File(p))

  private[config] implicit val logLevelReader: ConfigReader[Level] = ConfigReader[String] map (
      level => Level.valueOf(level))

  private[config] implicit val toReporterList: ConfigReader[List[MutantRunReporter]] =
    ConfigReader[List[String]].map(_.map {
      case MutantRunReporter.`consoleReporter` => new ConsoleReporter
    })

  private[config] implicit val exclusions: ConfigReader[Exclusions] =
    ConfigReader[List[String]]
      .map(warnInvalidExclusions)
      .map(exclusions => Exclusions(exclusions.toSet))

  private def warnInvalidExclusions(list: List[String]): List[String] = {
    val (valid, invalid) = list.partition(Mutation.mutations.contains)
    if (invalid.nonEmpty) {
      val errorMessage =
        s"""Invalid exclusion option(s): '${invalid.mkString(", ")}'
            |Valid exclusions are ${Mutation.mutations.mkString(", ")}""".stripMargin
      throw new Exception(errorMessage)
    }
    valid
  }
}
