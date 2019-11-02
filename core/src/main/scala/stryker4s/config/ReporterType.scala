package stryker4s.config

import stryker4s.report.DashboardReporter

sealed trait ReporterType {
  def name: String
}

case object ConsoleReporterType extends ReporterType {
  override val name: String = "console"
}

case object HtmlReporterType extends ReporterType {
  override val name: String = "html"
}

case object JsonReporterType extends ReporterType {
  override val name: String = "json"
}

case object DashboardReporterType extends ReporterType {
  override val name: String = "dashboard"

  def unapply(reporterType: ReporterType): Option[DashboardReporter] = reporterType match {
    case DashboardReporterType => DashboardReporter.resolveProvider()
    case _                     => None
  }
}
