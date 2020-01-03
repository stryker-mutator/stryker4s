package stryker4s.config.implicits

import java.nio.file.Path

import better.files.File
import com.typesafe.config.ConfigRenderOptions
import pureconfig.ConfigWriter
import stryker4s.config.{DashboardReportType, ExcludedMutations, ReporterType}
import pureconfig.generic.semiauto._

object ConfigWriterImplicits {
  implicit private[config] val fileWriter: ConfigWriter[File] =
    ConfigWriter[Path] contramap (_.path)

  implicit private[config] val exclusionsWriter: ConfigWriter[ExcludedMutations] =
    ConfigWriter[List[String]] contramap (_.exclusions.toList)

  implicit private[config] val reportersWriter: ConfigWriter[ReporterType] =
    deriveEnumerationWriter[ReporterType]

  implicit private[config] val dashboardReportTypeWriter: ConfigWriter[DashboardReportType] =
    deriveEnumerationWriter[DashboardReportType]

  private[config] val options: ConfigRenderOptions = ConfigRenderOptions
    .defaults()
    .setOriginComments(false)
    .setJson(false)
}
