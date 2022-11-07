package stryker4s.exception

import fs2.io.file.Path
import stryker4jvm.exception.Stryker4jvmException

class Stryker4sException(message: String) extends Stryker4jvmException(message) {
  def this(message: String, cause: Throwable) = {
    this(message)
    initCause(cause)
  }
}

final case class UnableToBuildPatternMatchException(file: Path, cause: Throwable)
    extends Stryker4sException(
      s"Failed to instrument mutants in `$file`.\nPlease open an issue on github and include the stacktrace and failed instrumentation code: https://github.com/stryker-mutator/stryker4s/issues/new",
      cause
    )
