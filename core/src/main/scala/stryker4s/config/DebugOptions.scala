package stryker4s.config

final case class DebugOptions(
    logTestRunnerStdout: Boolean = false,
    debugTestRunner: Boolean = false
)
