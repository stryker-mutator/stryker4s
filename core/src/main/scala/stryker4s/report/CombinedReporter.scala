package stryker4s.report

import stryker4s.model.{Mutant, MutantRunResult, MutantRunResults}

class CombinedReporter(reporters: Seq[MutantRunReporter]) extends MutantRunReporter {

  override def reportStartRun(mutant: Mutant): Unit = combineReporters(_.reportStartRun(mutant))

  override def reportFinishedMutation(result: MutantRunResult, totalMutants: Int): Unit =
    combineReporters(_.reportFinishedMutation(result, totalMutants))

  /**
    * Generate a report for each reporter that is available.
    */
  override def reportFinishedRun(runResults: MutantRunResults): Unit = combineReporters(_.reportFinishedRun(runResults))

  private def combineReporters(reportFunc: MutantRunReporter => Unit): Unit = reporters.foreach(reportFunc)
}
