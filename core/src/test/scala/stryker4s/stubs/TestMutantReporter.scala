package stryker4s.stubs

import stryker4s.model.MutantRunResults
import stryker4s.run.report.MutantRunReporter

class TestMutantReporter extends MutantRunReporter {
  var lastCall: Option[MutantRunResults] = None
  override def report(runResults: MutantRunResults): Unit = {
    lastCall = Some(runResults)
  }
}
