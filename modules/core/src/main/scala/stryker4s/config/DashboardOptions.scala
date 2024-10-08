package stryker4s.config

import sttp.model.Uri

final case class DashboardOptions(
    baseUrl: Uri,
    reportType: DashboardReportType,
    project: Option[String],
    version: Option[String],
    module: Option[String]
)

sealed trait DashboardReportType
case object Full extends DashboardReportType
case object MutationScoreOnly extends DashboardReportType
