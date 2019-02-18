package stryker4s.run.report

import stryker4s.model.{MutantRunResult, MutantRunResults}

trait MutantRunReporter {
  def reportFinishedRun(runResults: MutantRunResults): Unit

  def reportFinishedMutation(result: MutantRunResult): Unit
}
