package stryker4s.model

import fs2.io.file.Path
import stryker4s.testutil.Stryker4sSuite

import scala.meta.*

class MutatedFileTest extends Stryker4sSuite {
  describe("MutatedFile") {
    it("should return the new source codes as a string") {
      MutatedFile(
        Path("/blah/test"),
        q"class blah(x: String) { def hi() = x }",
        Seq.empty,
        Seq.empty,
        0
      ).mutatedSource shouldBe "class blah(x: String) { def hi() = x }"
    }
  }
}
