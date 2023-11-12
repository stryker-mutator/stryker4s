package stryker4s.files

import fs2.io.file.Path
import stryker4s.testkit.{FileUtil, Stryker4sIOSuite}

class GlobFileResolverTest extends Stryker4sIOSuite {

  describe("files") {
    val defaultGlob = Seq("**/main/**.scala")
    val filledDirPath: Path = FileUtil.getResource("fileTests/filledDir")
    val basePath: Path = filledDirPath / "src/main/scala"

    test("should not collect the baseDir") {
      val emptyDir = FileUtil.getResource("fileTests/emptyDir")
      val sut = new GlobFileResolver(emptyDir, defaultGlob)

      sut.files.compile.toVector
        .assertSameElementsAs(Seq.empty)
    }

    test("should find all scala files and not the non-scala files with a default glob") {
      val sut = new GlobFileResolver(filledDirPath, defaultGlob)

      sut.files.compile.toVector.assertSameElementsAs(
        Seq(
          basePath / "fileInRootSourceDir.scala",
          basePath / "package" / "someFile.scala",
          basePath / "package" / "secondFile.scala",
          basePath / "package" / "target.scala"
        )
      )
    }

    test("should find matching files with custom match pattern") {
      val sut = new GlobFileResolver(filledDirPath, Seq("src/**/second*.scala"))

      sut.files.compile.toVector
        .map(_.loneElement)
        .assertEquals(basePath / "package" / "secondFile.scala")
    }

    test("should only add a glob once even when it matches twice") {
      val sut = new GlobFileResolver(filledDirPath, Seq("**/someFile.scala", "src/main/scala/**/*.scala"))

      sut.files.compile.toVector.assertSameElementsAs(
        Seq(
          basePath / "package" / "someFile.scala",
          basePath / "package" / "secondFile.scala",
          basePath / "package" / "target.scala"
        )
      )
    }

    test("should exclude all files in the target folder") {
      val sut = new GlobFileResolver(filledDirPath.parent.value, defaultGlob)

      sut.files.compile.toVector.assertSameElementsAs(
        List(
          basePath / "fileInRootSourceDir.scala",
          basePath / "package" / "someFile.scala",
          basePath / "package" / "secondFile.scala",
          basePath / "package" / "target.scala"
        )
      )
    }
  }
}
