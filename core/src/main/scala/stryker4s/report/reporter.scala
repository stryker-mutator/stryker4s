package stryker4s.report
import stryker4s.model.{Mutant, MutantRunResult, MutantRunResults}

trait Reporter

trait ProgressReporter extends Reporter {
  def reportMutationStart(mutant: Mutant)

  def reportMutationComplete(result: MutantRunResult, totalMutants: Int): Unit
}

trait FinishedRunReporter extends Reporter {
  def reportRunFinished(runResults: MutantRunResults): Unit
}
