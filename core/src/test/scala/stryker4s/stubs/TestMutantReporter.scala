package stryker4s.stubs

import stryker4s.config.{Config, ConfigReader}
import stryker4s.model.MutantRunResults
import stryker4s.report.MutantRunReporter

class TestMutantReporter extends MutantRunReporter {
  var lastCall: Option[MutantRunResults] = None
  override def report(runResults: MutantRunResults)(implicit config: Config = ConfigReader.readConfig()): Unit = {
    lastCall = Some(runResults)
  }
}
