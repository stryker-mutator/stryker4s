package stryker4s.config.implicits

import java.nio.file.Path

import better.files.File
import pureconfig.ConfigReader
import stryker4s.config._

trait ConfigReaderImplicits {
  /** Converts a [[java.nio.file.Path]] to a [[better.files.File]] so PureConfig can read it
    *
    */
  implicit private[config] val toFileReader: ConfigReader[File] =
    ConfigReader[Path] map (p => File(p))

  implicit private[config] val toReporterList: ConfigReader[ReporterType] =
    ConfigReader[String] map {
      case ConsoleReporterType.name   => ConsoleReporterType
      case HtmlReporterType.name      => HtmlReporterType
      case JsonReporterType.name      => JsonReporterType
      case DashboardReporterType.name => DashboardReporterType
    }

  implicit private[config] val exclusions: ConfigReader[ExcludedMutations] =
    ConfigReader[List[String]] map (exclusions => ExcludedMutations(exclusions.toSet))
}
