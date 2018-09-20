package stryker4s.run.report

import stryker4s.config.Config
import stryker4s.model.MutantRunResults

trait MutantRunReporter {
  def report(runResults: MutantRunResults)(implicit config: Config): Unit
}

object MutantRunReporter {
  val consoleReporter: String = "console"
  val htmlReporter: String = "html"
}
