package stryker4s.sbt.testrunner

import stryker4s.testrunner.api.TestProcessProperties

object Context {
  def resolveSocketConfig(): Int = {
    sys.props(TestProcessProperties.port).toInt
  }
}
