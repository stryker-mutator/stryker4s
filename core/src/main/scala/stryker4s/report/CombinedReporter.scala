package stryker4s.report

import stryker4s.model.{Mutant, MutantRunResult, MutantRunResults}

class CombinedReporter(reporters: Seq[Reporter]) extends FinishedRunReporter with ProgressReporter {

  private[this] val progressReporters = reporters collect { case r: ProgressReporter   => r }
  private[this] val runResultReporters = reporters collect { case r: FinishedRunReporter => r }

  override def reportMutationStart(mutant: Mutant): Unit =
    progressReporters.foreach(_.reportMutationStart(mutant))

  override def reportMutationComplete(result: MutantRunResult, totalMutants: Int): Unit =
    progressReporters.foreach(_.reportMutationComplete(result, totalMutants))

  override def reportRunFinished(runResults: MutantRunResults): Unit =
    runResultReporters.foreach(_.reportRunFinished(runResults))

}
