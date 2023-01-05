package stryker4jvm.plugin.sbt.testrunner

import stryker4s.api.testprocess.TestProcessProperties

object Context {
  def resolveSocketConfig(): Int = {
    sys.props(TestProcessProperties.port).toInt
  }
}
