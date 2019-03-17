package stryker4s.report

import stryker4s.config.{Config, ConsoleReporterType}
import stryker4s.model.{Mutant, MutantRunResult, MutantRunResults}

class Reporter(implicit config: Config) extends FinishedRunReporter with ProgressReporter {

  def reporters: Seq[MutationRunReporter] = {
    config.reporters collect {
      case ConsoleReporterType => new ConsoleReporter()
    }
  }

  private[this] val progressReporters = reporters collect { case r: ProgressReporter       => r }
  private[this] val finishedRunReporters = reporters collect { case r: FinishedRunReporter => r }

  override def reportMutationStart(mutant: Mutant): Unit =
    progressReporters.foreach(_.reportMutationStart(mutant))

  override def reportMutationComplete(result: MutantRunResult, totalMutants: Int): Unit =
    progressReporters.foreach(_.reportMutationComplete(result, totalMutants))

  override def reportRunFinished(runResults: MutantRunResults): Unit =
    finishedRunReporters.foreach(_.reportRunFinished(runResults))

}
