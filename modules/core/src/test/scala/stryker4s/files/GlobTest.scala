package stryker4s.files

import fs2.io.file.Path
import stryker4s.testkit.Stryker4sSuite

import scala.util.Properties

class GlobTest extends Stryker4sSuite {
  describe("matcher") {

    val path = Path("").absolute

    describe("include patterns") {
      test("should match no files with empty glob") {
        val matcher = Glob.matcher(path, Seq.empty)

        assert(!matcher.matches(path / "foo.scala"))
      }

      test("should match files 0 levels deep") {
        val matcher = Glob.matcher(path, Seq("*.scala"))

        assert(matcher.matches(path / "foo.scala"))
      }

      test("should match on the second glob if the first doesn't match") {
        val matcher = Glob.matcher(path, Seq("*.sbt", "*.scala"))

        assert(matcher.matches(path / "foo.scala"))
      }

      test("should match files 1 level deep") {
        val matcher = Glob.matcher(path, Seq("src/*.scala"))

        val input = path / "src" / "foo.scala"

        assert(matcher.matches(input))
      }

      test("should match files multiple levels deep") {
        val matcher = Glob.matcher(path, Seq("src/**/*.scala"))

        val input = path / "src" / "main" / "scala" / "foo.scala"

        assert(matcher.matches(input))
      }

      test("should match on multiple patterns") {
        val matcher = Glob.matcher(path, Seq("**/someFile.scala", "**/secondFile.scala"))

        assert(matcher.matches(path / "src" / "main" / "scala" / "someFile.scala"))
        assert(matcher.matches(path / "src" / "main" / "scala" / "secondFile.scala"))
      }
    }

    describe("ignore patterns") {
      test("should exclude the file specified in the excluded pattern") {
        val matcher = Glob.matcher(path, Seq("**/someFile.scala", "**/secondFile.scala", "!**/someFile.scala"))

        assert(!matcher.matches(path / "src" / "someFile.scala"))
        assert(matcher.matches(path / "src" / "secondFile.scala"))
      }

      test("should exclude all files specified in the excluded pattern") {
        val matcher = Glob.matcher(
          path,
          Seq("**/someFile.scala", "**/secondFile.scala", "!**/someFile.scala", "!**/secondFile.scala")
        )

        assert(!matcher.matches(path / "src" / "someFile.scala"))
        assert(!matcher.matches(path / "src" / "secondFile.scala"))
      }

      test("should exclude all files based on a wildcard") {
        val matcher = Glob.matcher(
          path,
          Seq("**/someFile.scala", "**/secondFile.scala", "!**/*.scala")
        )

        assert(!matcher.matches(path / "src" / "someFile.scala"))
        assert(!matcher.matches(path / "src" / "secondFile.scala"))
      }

      test("should not exclude files if a non-matching ignore pattern is given") {
        val matcher = Glob.matcher(
          path,
          Seq("**/someFile.scala", "**/secondFile.scala", "!**/nonExistingFile.scala")
        )

        assert(matcher.matches(path / "src" / "someFile.scala"))
        assert(matcher.matches(path / "src" / "secondFile.scala"))
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
      if (Properties.isWin) escapeSequences
      else escapeSequences ++ linuxOnlySequences

    allSequences.foreach { escapeSequence =>
      test(s"should escape $escapeSequence in a path") {
        val matcher = Glob.matcher(path / escapeSequence, Seq(s"src/*.scala"))

        val input = path / escapeSequence / "src" / "foo.scala"

        assert(matcher.matches(input))
      }
    }
  }
}
