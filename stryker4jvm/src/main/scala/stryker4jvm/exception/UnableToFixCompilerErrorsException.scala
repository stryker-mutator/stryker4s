package stryker4jvm.exception

import cats.data.NonEmptyList
import cats.syntax.foldable.*
import stryker4jvm.model.CompilerErrMsg

final case class UnableToFixCompilerErrorsException(errs: NonEmptyList[CompilerErrMsg])
    extends Stryker4sException(
      "Unable to remove non-compiling mutants in the mutated files. As a work-around you can exclude them in the stryker.conf. Please report this issue at https://github.com/stryker-mutator/stryker4s/issues\n"
        + errs
          .map(err => s"${err.path}: '${err.msg}'")
          .mkString_("\n")
    )
