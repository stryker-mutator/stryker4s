package stryker4s.exception

import cats.data.NonEmptyList
import fs2.io.file.Path
import stryker4s.model.CompilerErrMsg
import stryker4s.testkit.Stryker4sSuite

import java.io.File.separator

class Stryker4sExceptionTest extends Stryker4sSuite {
  describe("UnableToBuildPatternMatchException") {
    test("should have the correct message") {
      val path = Path("foo/bar.scala")
      val sut = UnableToBuildPatternMatchException(path)
      assertNoDiff(
        sut.getMessage,
        s"Failed to instrument mutants in `foo${separator}bar.scala`.\nPlease open an issue on github and include the stacktrace and failed instrumentation code: https://github.com/stryker-mutator/stryker4s/issues/new"
      )
    }
  }

  describe("InitialTestRunFailedException") {
    test("should have the correct message") {
      assertNoDiff(InitialTestRunFailedException("testMsg").getMessage, "testMsg")
    }
  }

  describe("TestSetupException") {
    test("should have the correct message ") {
      assertNoDiff(
        TestSetupException("ProjectName").getMessage,
        "Could not setup mutation testing environment. Unable to resolve project ProjectName. This could be due to compile errors or misconfiguration of Stryker4s. See debug logs for more information."
      )
    }
  }

  describe("MutationRunFailedException") {
    test("should have the correct message") {
      assertNoDiff(MutationRunFailedException("xyz").getMessage, "xyz")
    }
  }
  describe("UnableToFixCompilerErrorsException") {
    test("should have a nicely formatted message") {
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

      assertNoDiff(
        UnableToFixCompilerErrorsException(errs).getMessage(),
        """Unable to remove non-compiling mutants in the mutated files. As a work-around you can exclude them in the stryker.conf. Please report this issue at https://github.com/stryker-mutator/stryker4s/issues
          |/src/main/scala/com/company/strykerTest/TestObj1.scala: 'value forall is not a member of object java.nio.file.Files'
          |/src/main/scala/com/company/strykerTest/TestObj1.scala: 'something something types'
          |/src/main/scala/com/company/strykerTest/TestObj2.scala: 'yet another error with symbols $#'%%$~@1'""".stripMargin
      )
    }
  }
}
