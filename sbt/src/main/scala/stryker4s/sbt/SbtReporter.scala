package stryker4s.sbt

import stryker4s.model.MutantRunResults
import stryker4s.run.report.MutantRunReporter

class SbtReporter extends MutantRunReporter {

  // TODO: report info through sbt console
  override def report(runResults: MutantRunResults): Unit = {
    println("REPORT")
    println(runResults)
  }

}
