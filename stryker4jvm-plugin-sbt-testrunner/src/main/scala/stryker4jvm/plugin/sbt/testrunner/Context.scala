package stryker4jvm.plugin.sbt.testrunner

import stryker4jvm.api.testprocess.TestProcessProperties

object Context {
  def resolveSocketConfig(): Int = {
    sys.props(TestProcessProperties.port).toInt
  }
}
