package stryker4s.sbt.testrunner

import stryker4s.api.testprocess.{TestProcessConfig, TestProcessProperties}

object Context {
  def resolveSocketConfig(): TestProcessConfig = {
    val port = sys.props(TestProcessProperties.port).toInt
    TestProcessConfig(port = port)
  }
}
