package stryker4s.testutil.stubs

import stryker4s.run.RollbackHandler
import cats.data.NonEmptyList
import stryker4s.model.CompilerErrMsg
import stryker4s.run.RollbackResult
import cats.syntax.either.*

object RollbackHandlerStub {
  def alwaysSuccessful(): RollbackHandler = (_, allFiles) => RollbackResult(allFiles, Map.empty).asRight

  def withResult(result: Either[NonEmptyList[CompilerErrMsg], RollbackResult]): RollbackHandler = (_, _) => result

}
