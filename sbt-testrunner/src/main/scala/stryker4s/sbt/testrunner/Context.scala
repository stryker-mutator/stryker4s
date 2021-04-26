package stryker4s.sbt.testrunner

import stryker4s.api.testprocess.{TestProcessProperties}

object Context {
  def resolveSocketConfig(): Int = {
    sys.props(TestProcessProperties.port).toInt
  }
}
