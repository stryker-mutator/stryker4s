package stryker4jvm.exception

import cats.data.NonEmptyList
import fs2.io.file.Path
import stryker4jvm.model.CompilerErrMsg
import stryker4jvm.testutil.Stryker4sSuite

class Stryker4sExceptionTest extends Stryker4sSuite {
  describe("UnableToBuildPatternMatchException") {
    it("should have the correct message") {
      val parent = new RuntimeException("parent")
      val sut = UnableToBuildPatternMatchException(
        Path("foo/bar.scala"),
        parent
      )
      sut.getMessage shouldBe s"Failed to instrument mutants in `foo/bar.scala`.\nPlease open an issue on github and include the stacktrace and failed instrumentation code: https://github.com/stryker-mutator/stryker4s/issues/new"
      sut.cause shouldBe parent
    }
  }

  describe("InitialTestRunFailedException") {
    it("should have the correct message") {
      InitialTestRunFailedException("testMsg").getMessage shouldBe "testMsg"
    }
  }

  describe("TestSetupException") {
    it("should have the correct message ") {
      TestSetupException("ProjectName").getMessage shouldBe
        "Could not setup mutation testing environment. Unable to resolve project ProjectName. This could be due to compile errors or misconfiguration of Stryker4s. See debug logs for more information."
    }
  }

  describe("MutationRunFailedException") {
    it("should have the correct message") {
      MutationRunFailedException("xyz").getMessage shouldBe "xyz"
    }
  }
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
