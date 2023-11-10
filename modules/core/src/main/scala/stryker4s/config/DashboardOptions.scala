package stryker4s.config

import cats.syntax.option.*
import sttp.client3.UriContext
import sttp.model.Uri

final case class DashboardOptions(
    baseUrl: Uri = uri"https://dashboard.stryker-mutator.io",
    reportType: DashboardReportType = Full,
    project: Option[String] = none,
    version: Option[String] = none,
    module: Option[String] = none
)

sealed trait DashboardReportType
case object Full extends DashboardReportType
case object MutationScoreOnly extends DashboardReportType
