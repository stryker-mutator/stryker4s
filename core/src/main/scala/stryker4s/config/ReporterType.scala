package stryker4s.config

import stryker4s.report.DashboardReporter

sealed trait ReporterType

case object Console extends ReporterType

case object Html extends ReporterType

case object Json extends ReporterType

case object Dashboard extends ReporterType {
  def unapply(reporterType: ReporterType): Option[DashboardReporter] = reporterType match {
    case Dashboard => DashboardReporter.resolveProvider()
    case _         => None
  }
}
