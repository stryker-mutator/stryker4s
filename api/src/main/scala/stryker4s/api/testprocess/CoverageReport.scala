package stryker4s.api.testprocess

case class CoverageReport(report: Map[Int, Seq[Fingerprint]]) extends AnyVal

object CoverageReport {
  def apply(testRunMap: CoverageTestRunMap): CoverageReport = {
    val map = testRunMap.fingerprints.map { case (id, Fingerprints(fingerPrintIds)) =>
      id -> fingerPrintIds.map(testRunMap.fingerprintIds)
    }
    CoverageReport(map)
  }
}
