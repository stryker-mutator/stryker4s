package stryker4s.model

import cats.syntax.show.*
import stryker4s.testutil.Stryker4sSuite

class CompilerErrMsgTest extends Stryker4sSuite {
  describe("CompilerErrMsgTest") {
    it("should have a nicely formatted show") {
      CompilerErrMsg(
        msg = "value forall is not a member of object java.nio.file.Files",
        path = "/src/main/scala/com/company/strykerTest/TestObj1.scala",
        line = 123
      ).show shouldBe "L123: value forall is not a member of object java.nio.file.Files"
    }
  }
}
