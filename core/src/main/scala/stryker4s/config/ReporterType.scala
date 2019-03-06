package stryker4s.config

sealed trait ReporterType {
  val name: String
}

case object ConsoleReporterType extends ReporterType {
  override val name: String = "console"
}
