package stryker4s.testrunner.api

import stryker4s.model.MutantId

case class CoverageReport(report: Map[MutantId, Seq[TestFile]]) extends AnyVal

object CoverageReport {
  def apply(testRunMap: CoverageTestNameMap): CoverageReport = {
    val map = testRunMap.testNames.map { case (id, TestNames(testNameIds)) =>
      id -> testNameIds.map(testRunMap.testNameIds)
    }
    CoverageReport(map)
  }
}
