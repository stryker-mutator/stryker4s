package stryker4s.report
import stryker4s.model.{Mutant, MutantRunResult, MutantRunResults}

trait MutationRunReporter

trait ProgressReporter extends MutationRunReporter {
  def reportMutationStart(mutant: Mutant)

  def reportMutationComplete(result: MutantRunResult, totalMutants: Int): Unit
}

trait FinishedRunReporter extends MutationRunReporter {
  def reportRunFinished(runResults: MutantRunResults): Unit
}
