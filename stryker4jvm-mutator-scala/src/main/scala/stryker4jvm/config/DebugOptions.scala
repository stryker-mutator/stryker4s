package stryker4jvm.config

final case class DebugOptions(
    logTestRunnerStdout: Boolean = false,
    debugTestRunner: Boolean = false
)
