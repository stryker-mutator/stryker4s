package stryker4s.testutil.stubs

import cats.data.NonEmptyList
import cats.syntax.either.*
import stryker4s.model.CompilerErrMsg
import stryker4s.run.{RollbackHandler, RollbackResult}

object RollbackHandlerStub {
  def alwaysSuccessful(): RollbackHandler = (_, allFiles) => RollbackResult(allFiles, Map.empty).asRight

  def withResult(result: Either[NonEmptyList[CompilerErrMsg], RollbackResult]): RollbackHandler = (_, _) => result

}
