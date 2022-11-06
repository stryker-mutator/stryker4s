package stryker4jvm.reporting.model

import stryker4jvm.config.DashboardReportType
import sttp.model.Uri

final case class DashboardConfig(
    apiKey: String,
    baseUrl: Uri,
    reportType: DashboardReportType,
    project: String,
    version: String,
    module: Option[String]
)
