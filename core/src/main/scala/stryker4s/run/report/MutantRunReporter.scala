package stryker4s.run.report

import stryker4s.model.MutantRunResults

trait MutantRunReporter {
  val name: String

  def report(runResults: MutantRunResults): Unit
}
