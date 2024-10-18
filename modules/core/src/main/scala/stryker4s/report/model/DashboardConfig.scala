package stryker4s.report.model

import stryker4s.config.DashboardReportType
import sttp.model.Uri

final case class DashboardConfig(
    apiKey: String,
    baseUrl: Uri,
    reportType: DashboardReportType,
    project: String,
    version: String,
    module: Option[String]
)
