package stryker4s.config

final case class DashboardOptions(
    baseUrl: String = "https://dashboard.stryker-mutator.io",
    reportType: DashboardReportType = Full,
    project: Option[String] = None,
    version: Option[String] = None,
    module: Option[String] = None
)

sealed trait DashboardReportType
case object Full extends DashboardReportType
case object MutationScoreOnly extends DashboardReportType
