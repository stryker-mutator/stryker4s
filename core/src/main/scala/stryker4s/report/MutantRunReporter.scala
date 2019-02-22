package stryker4s.report

import stryker4s.model.{Mutant, MutantRunResult, MutantRunResults}

trait MutantRunReporter {

  def reportStartRun(mutant: Mutant)

  def reportFinishedRun(runResults: MutantRunResults): Unit

  def reportFinishedMutation(result: MutantRunResult, totalMutants: Int): Unit
}
