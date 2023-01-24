package stryker4jvm.api.testprocess

case class CoverageReport(report: Map[Int, Seq[String]]) extends AnyVal

object CoverageReport {
  def apply(testRunMap: CoverageTestNameMap): CoverageReport = {
    val map = testRunMap.testNames.map { case (id, TestNames(testNameIds)) =>
      id -> testNameIds.map(testRunMap.testNameIds)
    }
    CoverageReport(map)
  }
}
