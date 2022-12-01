package stryker4jvm.mutator.scala.exception

import cats.data.NonEmptyList
import fs2.io.file.Path
import stryker4jvm.mutator.scala.testutil.Stryker4sSuite

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
}
