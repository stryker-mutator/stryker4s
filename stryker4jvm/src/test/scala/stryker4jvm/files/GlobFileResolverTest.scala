package stryker4jvm.files

import fs2.io.file.Path
import stryker4jvm.mutator.scala.scalatest.FileUtil

class GlobFileResolverTest extends Stryker4jvmIOSuite {

  describe("files") {
    val defaultGlob = Seq("**/main/**.scala")
    val filledDirPath: Path = FileUtil.getResource("fileTests/filledDir")
    val basePath: Path = filledDirPath / "src/main/scala"

    it("should not collect the baseDir") {
      val emptyDir = FileUtil.getResource("fileTests/emptyDir")
      val sut = new GlobFileResolver(emptyDir, defaultGlob)

      sut.files.compile.toVector
        .asserting(_ should be(empty))
    }

    it("should find all scala files and not the non-scala files with a default glob") {
      val sut = new GlobFileResolver(filledDirPath, defaultGlob)

      sut.files.compile.toVector.asserting { results =>
        results should (
          contain.only(
            basePath / "fileInRootSourceDir.scala",
            basePath / "package" / "someFile.scala",
            basePath / "package" / "secondFile.scala",
            basePath / "package" / "target.scala"
          )
        )
      }
    }

    it("should find matching files with custom match pattern") {
      val sut = new GlobFileResolver(filledDirPath, Seq("src/**/second*.scala"))

      sut.files.compile.toVector
        .map(_.loneElement)
        .asserting { onlyResult =>
          onlyResult should equal(basePath / "package" / "secondFile.scala")
        }
    }

    it("should only add a glob once even when it matches twice") {
      val sut = new GlobFileResolver(filledDirPath, Seq("**/someFile.scala", "src/main/scala/**/*.scala"))

      sut.files.compile.toVector.asserting { results =>
        results should (contain.only(
          basePath / "package" / "someFile.scala",
          basePath / "package" / "secondFile.scala",
          basePath / "package" / "target.scala"
        ))
      }
    }

    it("should exclude all files in the target folder") {
      val sut = new GlobFileResolver(filledDirPath.parent.get, defaultGlob)

      sut.files.compile.toVector.asserting { results =>
        results should (
          contain.only(
            basePath / "fileInRootSourceDir.scala",
            basePath / "package" / "someFile.scala",
            basePath / "package" / "secondFile.scala",
            basePath / "package" / "target.scala"
          )
        )
      }
    }
  }
}
