package stryker4s

import stryker4s.model.InitialTestRunCoverageReport

package object run {
  type InitialTestRunResult = Either[Boolean, InitialTestRunCoverageReport]

  type TestRunnerPool = ResourcePool[TestRunner]
}
