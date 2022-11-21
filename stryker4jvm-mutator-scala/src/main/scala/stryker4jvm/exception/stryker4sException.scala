package stryker4jvm.exception

import cats.data.NonEmptyList
import cats.syntax.foldable.*
import fs2.io.file.Path

import scala.util.control.NoStackTrace
import stryker4jvm.core.exception.Stryker4jvmException

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

final case class InitialTestRunFailedException(message: String) extends Stryker4sException(message) with NoStackTrace

final case class TestSetupException(name: String)
    extends Stryker4sException(
      s"Could not setup mutation testing environment. Unable to resolve project $name. This could be due to compile errors or misconfiguration of Stryker4s. See debug logs for more information."
    )

final case class MutationRunFailedException(message: String) extends Stryker4sException(message)
