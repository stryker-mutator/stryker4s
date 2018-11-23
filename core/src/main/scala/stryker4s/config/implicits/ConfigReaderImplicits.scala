package stryker4s.config.implicits
import java.nio.file.Path

import better.files.File
import grizzled.slf4j.Logging
import org.apache.logging.log4j.Level
import pureconfig.{ConfigCursor, ConfigReader}
import pureconfig.error.ConfigReaderFailures
import stryker4s.extensions.exceptions.InvalidExclusionsFailure
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
    ConfigReader.fromCursor[List[String]](errorOnInvalidExclusions)
      .map(exclusions => Exclusions(exclusions.toSet))

  private def errorOnInvalidExclusions(configCursor: ConfigCursor): Either[ConfigReaderFailures, List[String]] = {
    configCursor.asList.flatMap( cursorList => {
      val partitioned = cursorList.map(_.asString).partition {
        case Left(_)  => false
        case Right(s) => Mutation.mutations.contains(s)
      }
      val validExclusions = partitioned._1.map(_.right.get)
      val invalidExclusions = partitioned._2.filter(_.isRight).map(_.right.get)
      val otherFailures = partitioned._2.filter(_.isLeft).flatMap(_.left.get.toList)

      if(invalidExclusions.isEmpty && otherFailures.isEmpty) {
        Right(validExclusions)
      } else {
        val failure = configCursor.failureFor(InvalidExclusionsFailure(invalidExclusions))
        Left(ConfigReaderFailures(failure, otherFailures))
      }
    })
  }
}
