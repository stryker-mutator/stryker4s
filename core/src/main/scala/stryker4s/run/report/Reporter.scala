package stryker4s.run.report
import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.model.MutantRunResults

class Reporter extends Logging {

  /**
    * Generate a report for each reporter that is available.
    */
  def report(runResult: MutantRunResults)(implicit config: Config): Unit = {
    config.reporters.foreach { _.report(runResult) }
  }
}
