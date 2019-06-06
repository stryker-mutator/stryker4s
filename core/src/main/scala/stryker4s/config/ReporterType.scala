package stryker4s.config

sealed trait ReporterType {
  val name: String
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
}
