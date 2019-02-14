package stryker4s.testutil.stubs

import stryker4s.config.Config
import stryker4s.model.MutantRunResults
import stryker4s.run.report.{MutantRunReporter, Reporter}

class TestReporter extends Reporter {
  val testMutantReporter = new TestMutantReporter

  override def report(runResult: MutantRunResults)(implicit config: Config): Unit = {
    testMutantReporter.reportFinishedRun(runResult)
  }
}

class TestMutantReporter extends MutantRunReporter {
  val name = "testMutant"
  var lastCall: Option[MutantRunResults] = None

  override def reportFinishedRun(runResults: MutantRunResults)(implicit config: Config): Unit = {
    lastCall = Some(runResults)
  }
}
