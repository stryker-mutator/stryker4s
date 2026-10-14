package stryker4s.testrunner.api

import stryker4s.model.MutantId

case class CoverageReport(report: Map[MutantId, Seq[TestFile]]) extends AnyVal

object CoverageReport {
  def apply(testRunMap: CoverageTestNameMap): CoverageReport = {
    val map = testRunMap.testsByMutant.map { case (id, TestFileIds(testFileIds)) =>
      id -> testFileIds.map(testRunMap.testFiles)
    }
    CoverageReport(map)
  }

  lazy val empty: CoverageReport = CoverageReport(Map.empty[MutantId, Seq[TestFile]])
}
