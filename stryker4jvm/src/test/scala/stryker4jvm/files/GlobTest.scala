package stryker4jvm.files

import fs2.io.file.Path
import stryker4jvm.testutil.Stryker4jvmSuite

class GlobTest extends Stryker4jvmSuite {
  describe("matcher") {

    val path = Path("").absolute

    describe("include patterns") {
      it("should match no files with empty glob") {
        val matcher = Glob.matcher(path, Seq.empty)

        matcher.matches(path / "foo.scala") shouldBe false
      }

      it("should match files 0 levels deep") {
        val matcher = Glob.matcher(path, Seq("*.scala"))

        matcher.matches(path / "foo.scala") shouldBe true
      }

      it("should match on the second glob if the first doesn't match") {
        val matcher = Glob.matcher(path, Seq("*.sbt", "*.scala"))

        matcher.matches(path / "foo.scala") shouldBe true
      }

      it("should match files 1 level deep") {
        val matcher = Glob.matcher(path, Seq("src/*.scala"))

        val input = path / "src" / "foo.scala"

        matcher.matches(input) shouldBe true
      }

      it("should match files multiple levels deep") {
        val matcher = Glob.matcher(path, Seq("src/**/*.scala"))

        val input = path / "src" / "main" / "scala" / "foo.scala"

        matcher.matches(input) shouldBe true
      }

      it("should match on multiple patterns") {
        val matcher = Glob.matcher(path, Seq("**/someFile.scala", "**/secondFile.scala"))

        matcher.matches(path / "src" / "main" / "scala" / "someFile.scala") shouldBe true
        matcher.matches(path / "src" / "main" / "scala" / "secondFile.scala") shouldBe true
      }
    }

    describe("ignore patterns") {
      it("should exclude the file specified in the excluded pattern") {
        val matcher = Glob.matcher(path, Seq("**/someFile.scala", "**/secondFile.scala", "!**/someFile.scala"))

        matcher.matches(path / "src" / "someFile.scala") shouldBe false
        matcher.matches(path / "src" / "secondFile.scala") shouldBe true
      }

      it("should exclude all files specified in the excluded pattern") {
        val matcher = Glob.matcher(
          path,
          Seq("**/someFile.scala", "**/secondFile.scala", "!**/someFile.scala", "!**/secondFile.scala")
        )

        matcher.matches(path / "src" / "someFile.scala") shouldBe false
        matcher.matches(path / "src" / "secondFile.scala") shouldBe false
      }

      it("should exclude all files based on a wildcard") {
        val matcher = Glob.matcher(
          path,
          Seq("**/someFile.scala", "**/secondFile.scala", "!**/*.scala")
        )

        matcher.matches(path / "src" / "someFile.scala") shouldBe false
        matcher.matches(path / "src" / "secondFile.scala") shouldBe false
      }

      it("should not exclude files if a non-matching ignore pattern is given") {
        val matcher = Glob.matcher(
          path,
          Seq("**/someFile.scala", "**/secondFile.scala", "!**/nonExistingFile.scala")
        )

        matcher.matches(path / "src" / "someFile.scala") shouldBe true
        matcher.matches(path / "src" / "secondFile.scala") shouldBe true
      }
    }

    val escapeSequences = Seq(
      "\\",
      "{",
      "}",
      "[",
      "]"
    )

    // * and ? are invalid in Windows paths, so skip them in the tests as they can never be constructed anyway
    val linuxOnlySequences = Seq("*", "?")

    val allSequences =
      if (sys.props("os.name").toLowerCase.contains("windows")) escapeSequences
      else escapeSequences ++ linuxOnlySequences

    allSequences.foreach { escapeSequence =>
      it(s"should escape $escapeSequence in a path") {
        val matcher = Glob.matcher(path / escapeSequence, Seq(s"src/*.scala"))

        val input = path / escapeSequence / "src" / "foo.scala"

        matcher.matches(input) shouldBe true
      }
    }
  }

}
