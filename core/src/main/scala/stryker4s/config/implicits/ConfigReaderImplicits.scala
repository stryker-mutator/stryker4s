package stryker4s.config.implicits

import java.nio.file.Path

import better.files.File
import pureconfig.ConfigReader
import stryker4s.config._
import pureconfig.generic.semiauto._

trait ConfigReaderImplicits {

  /** Converts a [[java.nio.file.Path]] to a [[better.files.File]] so PureConfig can read it
    */
  implicit private[config] val toFileReader: ConfigReader[File] =
    ConfigReader[Path] map (p => File(p))

  implicit private[config] val toReporterList: ConfigReader[ReporterType] =
    deriveEnumerationReader[ReporterType]

  implicit private[config] val toDashboardOptions: ConfigReader[DashboardReportType] =
    deriveEnumerationReader[DashboardReportType]

  implicit private[config] val exclusions: ConfigReader[ExcludedMutations] =
    ConfigReader[List[String]] map (exclusions => ExcludedMutations(exclusions.toSet))

  implicit private[config] val uriReader = pureconfig.module.sttp.reader

}
