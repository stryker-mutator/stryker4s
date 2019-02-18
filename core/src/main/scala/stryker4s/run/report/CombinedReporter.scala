package stryker4s.run.report

import stryker4s.model.{MutantRunResult, MutantRunResults}

class CombinedReporter(reporters: Seq[MutantRunReporter]) extends MutantRunReporter {

  /**
    * Generate a report for each reporter that is available.
    */
  override def reportFinishedRun(runResults: MutantRunResults): Unit = reporters.foreach(_.reportFinishedRun(runResults))

  override def reportFinishedMutation(result: MutantRunResult): Unit = reporters.foreach(_.reportFinishedMutation(result))

}
