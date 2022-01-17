package stryker4s.extension.exception

import cats.data.NonEmptyList
import cats.syntax.foldable.*
import stryker4s.model.CompilerErrMsg

import scala.util.control.NoStackTrace

sealed abstract class Stryker4sException(message: String) extends Exception(message)

final case class UnableToBuildPatternMatchException() extends Stryker4sException("Unable to build pattern match")

final case class InitialTestRunFailedException(message: String) extends Stryker4sException(message) with NoStackTrace

final case class TestSetupException(name: String)
    extends Stryker4sException(
      s"Could not setup mutation testing environment. Unable to resolve project $name. This could be due to compile errors or misconfiguration of Stryker4s. See debug logs for more information."
    )

final case class MutationRunFailedException(message: String) extends Stryker4sException(message)

final case class UnableToFixCompilerErrorsException(errs: NonEmptyList[CompilerErrMsg])
    extends Stryker4sException(
      "Unable to remove non-compiling mutants in the mutated files. As a work-around you can exclude them in the stryker.conf. Please report this issue at https://github.com/stryker-mutator/stryker4s/issues\n"
        + errs
          .map(err => s"${err.path}: '${err.msg}'")
          .mkString_("\n")
    )
