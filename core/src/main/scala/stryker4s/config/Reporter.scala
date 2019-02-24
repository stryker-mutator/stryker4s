package stryker4s.config

sealed trait Reporter {
  val name: String
}

case object ConsoleReporter extends Reporter {
  override val name: String = "console"
}
