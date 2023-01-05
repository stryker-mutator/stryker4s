package stryker4jvm.exception

import cats.data.NonEmptyList
import stryker4jvm.model.CompilerErrMsg
import stryker4jvm.testutil.Stryker4jvmSuite

class UnableToFixCompilerErrorsExceptionTest extends Stryker4jvmSuite {
  describe("UnableToFixCompilerErrorsException") {
    it("should have a nicely formatted message") {
      val errs = NonEmptyList.of(
        CompilerErrMsg(
          msg = "value forall is not a member of object java.nio.file.Files",
          path = "/src/main/scala/com/company/strykerTest/TestObj1.scala",
          line = 123
        ),
        CompilerErrMsg(
          msg = "something something types",
          path = "/src/main/scala/com/company/strykerTest/TestObj1.scala",
          line = 2
        ),
        CompilerErrMsg(
          msg = "yet another error with symbols $#'%%$~@1",
          path = "/src/main/scala/com/company/strykerTest/TestObj2.scala",
          line = 10000
        )
      )

      UnableToFixCompilerErrorsException(errs).getMessage shouldBe
        """Unable to remove non-compiling mutants in the mutated files. As a work-around you can exclude them in the stryker.conf. Please report this issue at https://github.com/stryker-mutator/stryker4s/issues
          |/src/main/scala/com/company/strykerTest/TestObj1.scala: 'value forall is not a member of object java.nio.file.Files'
          |/src/main/scala/com/company/strykerTest/TestObj1.scala: 'something something types'
          |/src/main/scala/com/company/strykerTest/TestObj2.scala: 'yet another error with symbols $#'%%$~@1'""".stripMargin
    }
  }
}
