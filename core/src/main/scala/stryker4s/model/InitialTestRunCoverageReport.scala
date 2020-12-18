package stryker4s.model

import stryker4s.api.testprocess.Fingerprint

case class InitialTestRunCoverageReport(
    isSuccessful: Boolean,
    firstRun: Map[Int, Seq[Fingerprint]],
    secondRun: Map[Int, Seq[Fingerprint]]
)
