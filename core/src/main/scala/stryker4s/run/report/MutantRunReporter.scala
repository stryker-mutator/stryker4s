package stryker4s.run.report

import stryker4s.model.MutantRunResults

trait MutantRunReporter {
  def report(runResults: MutantRunResults): Unit
}

object MutantRunReporter {
  val consoleReporter: String = "console"
}