package stryker4s.config

// TODO: remove default values
final case class DebugOptions(
    logTestRunnerStdout: Boolean = false,
    debugTestRunner: Boolean = false
)
