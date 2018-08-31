package stryker4s.run.report
import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.model.MutantRunResults

class Reporter extends Logging {

  /**
    * Generate a report for each reporter that is available.
    * If no reporter was found use default console reporter.
    */
  def report(runResult: MutantRunResults)(implicit config: Config): Unit = {
    config.reporters.foreach {
      _.toLowerCase() match {
        case "console" => new ConsoleReporter().report(runResult)
        case _ =>
          warn(s"Configured reporter(s) [${config.reporters.mkString(", ")}] were not found.")
          warn("Using console reporter.")
          new ConsoleReporter().report(runResult)
      }
    }
  }
}
