package stryker4s.run.report
import stryker4s.config.Config
import stryker4s.model.MutantRunResults

class Reporter {

  /**
    * Generate a report for each reporter that is available.
    *
    * @param runResult
    * @param config
    */
  def report(runResult: MutantRunResults)(implicit config: Config): Unit = {
    config.reporters.foreach {
      _.toLowerCase() match {
        case "console" => new ConsoleReporter().report(runResult)
      }
    }
  }
}
