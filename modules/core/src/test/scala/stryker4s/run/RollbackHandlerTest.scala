package stryker4s.run

import cats.data.{NonEmptyList, NonEmptyVector}
import cats.syntax.option.*
import fansi.Color
import fs2.Pure
import fs2.io.file.Path
import mutationtesting.MutantStatus
import stryker4s.config.Config
import stryker4s.extension.TreeExtensions.FindExtension
import stryker4s.model.*
import stryker4s.mutants.tree.{InstrumenterOptions, MutantInstrumenter}
import stryker4s.testkit.{FileUtil, LogMatchers, Stryker4sIOSuite}

import scala.meta.*

class RollbackHandlerTest extends Stryker4sIOSuite with LogMatchers {
  implicit val config: Config = Config.default

  val sut = RollbackHandler(new MutantInstrumenter(InstrumenterOptions.testRunner))
  test("rollbackFiles should remove a non-compiling mutant") {
    rollbackableTree.asserting { tree =>
      val mutantTree = tree.find("Files.forall(Paths.get(a))".parseTerm).value
      val mutantMetadata =
        MutantMetadata(mutantTree.syntax, "Files.forall(Paths.get(a))", "MethodExpression", mutantTree.pos, none)
      val mutants = NonEmptyVector.of(
        MutantWithId(
          MutantId(1),
          MutatedCode(
            mutantTree,
            mutantMetadata
          )
        )
      )
      val path = Path("bar/baz.scala")
      val allFiles = Vector(MutatedFile(path, tree, mutants))
      val errors = NonEmptyList.of(
        CompilerErrMsg("error", path.toString, mutantMetadata.location.start.line),
        CompilerErrMsg("error2", path.toString, mutantMetadata.location.start.line)
      )

      val result = sut.rollbackFiles(errors, allFiles).value

      assertEquals(
        result.compileErrors.loneElement,
        path -> mutants
          .map(
            _.toMutantResult(
              MutantStatus.CompileError,
              statusReason = s"L${mutantMetadata.location.start.line}: error2".some
            )
          )
          .toVector
      )
      assertEquals(result.newFiles, Seq.empty)
      assertLoggedInfo(
        s"${Color.Red("2")} mutant(s) gave a compiler error. They will be marked as such in the report."
      )
      assertLoggedDebug(s"Removing 2 mutants with compile errors from $path: 'L7: error', 'L7: error2'")
    }
  }

  test("rollbackFiles should return a Left if no mutants were removed") {
    rollbackableTree.asserting { tree =>
      val mutantTree = tree.find("Files.forall(Paths.get(a))".parseTerm).value
      val mutantMetadata =
        MutantMetadata(mutantTree.syntax, "Files.forall(Paths.get(a))", "MethodExpression", mutantTree.pos, none)

      //
      val nonExistentMutantId = MutantId(2)
      val mutants = NonEmptyVector.of(
        MutantWithId(
          nonExistentMutantId,
          MutatedCode(
            mutantTree,
            mutantMetadata
          )
        )
      )
      val path = Path("bar/baz.scala")
      val allFiles = Vector(MutatedFile(path, tree, mutants))
      val errors = NonEmptyList.of(CompilerErrMsg("error", path.toString, mutantMetadata.location.start.line))

      val result = sut.rollbackFiles(errors, allFiles).leftValue

      assertEquals(result, errors)
      assertLoggedError(s"No mutants were removed in $path even though there were 1 compile errors")
    }
  }

  test("rollbackFiles should filter out fixed files of the rollbackResult") {
    rollbackableTree.asserting { tree =>
      val mutantTree = tree.find("Files.forall(Paths.get(a))".parseTerm).value
      val mutantMetadata =
        MutantMetadata(mutantTree.syntax, "Files.forall(Paths.get(a))", "MethodExpression", mutantTree.pos, none)
      val mutants = NonEmptyVector.of(
        MutantWithId(
          MutantId(1),
          MutatedCode(
            mutantTree,
            mutantMetadata
          )
        )
      )
      val path = Path("bar/baz.scala")
      val secondPath = Path("bar/baz2.scala")
      val allFiles = Vector(MutatedFile(path, tree, mutants), MutatedFile(secondPath, tree, mutants))
      val errors = NonEmptyList.of(CompilerErrMsg("error", path.toString, mutantMetadata.location.start.line))

      val result = sut.rollbackFiles(errors, allFiles).value

      assertEquals(
        result.compileErrors.loneElement,
        path -> mutants
          .map(
            _.toMutantResult(
              MutantStatus.CompileError,
              statusReason = s"L${mutantMetadata.location.start.line}: error".some
            )
          )
          .toVector
      )
      assertEquals(result.newFiles, allFiles.tail)
    }
  }

  test("rollbackFiles should not crash when a compile error also covers the original (wildcard) case") {
    rollbackableTree.asserting { tree =>
      val mutantTree = tree.find("Files.forall(Paths.get(a))".parseTerm).value
      val mutantMetadata =
        MutantMetadata(mutantTree.syntax, "Files.forall(Paths.get(a))", "MethodExpression", mutantTree.pos, none)
      val mutants = NonEmptyVector.of(
        MutantWithId(
          MutantId(1),
          MutatedCode(
            mutantTree,
            mutantMetadata
          )
        )
      )
      val path = Path("bar/baz.scala")
      val allFiles = Vector(MutatedFile(path, tree, mutants))
      val errors = NonEmptyList.of(
        CompilerErrMsg("error", path.toString, mutantMetadata.location.start.line),
        CompilerErrMsg("error in original", path.toString, 10)
      )

      val result = sut.rollbackFiles(errors, allFiles).value

      assertEquals(
        result.compileErrors.loneElement,
        path -> mutants
          .map(
            _.toMutantResult(
              MutantStatus.CompileError,
              statusReason = s"L${mutantMetadata.location.start.line}: error".some
            )
          )
          .toVector
      )
    }
  }

  test("rollbackFiles should remove a non-compiling mutant when using character offset instead of line number") {
    rollbackableTree.asserting { tree =>
      val mutantTree = tree.find("Files.forall(Paths.get(a))".parseTerm).value
      val mutantMetadata =
        MutantMetadata(mutantTree.syntax, "Files.forall(Paths.get(a))", "MethodExpression", mutantTree.pos, none)
      val mutants = NonEmptyVector.of(
        MutantWithId(
          MutantId(1),
          MutatedCode(
            mutantTree,
            mutantMetadata
          )
        )
      )
      val path = Path("bar/baz.scala")
      val allFiles = Vector(MutatedFile(path, tree, mutants))
      // Use line=1 (wrong line, would not match by line-number), but offset points into the case statement
      val errors = NonEmptyList.of(
        CompilerErrMsg("error", path.toString, 1, mutantTree.pos.start.some)
      )

      val result = sut.rollbackFiles(errors, allFiles).value

      assertEquals(
        result.compileErrors.loneElement,
        path -> mutants
          .map(
            _.toMutantResult(
              MutantStatus.CompileError,
              statusReason = s"L1: error".some
            )
          )
          .toVector
      )
      assertEquals(result.newFiles, Seq.empty)
    }
  }
  test(
    "rollbackFiles should remove non-compiling mutants when the on-disk splice differs from the pretty-printed tree"
  ) {
    val blankLines = "\n" * 20
    val source = s"""object Foo {$blankLines
                    |  def bar(a: String): Boolean = {
                    |    val p = a.trim
                    |    x >= 15
                    |  }
                    |}""".stripMargin.parseSource
    val original = source.find("x >= 15".parseTerm).value
    val mutants = NonEmptyVector.of(
      MutantWithId(
        MutantId(0),
        MutatedCode("x > 15".parseTerm, MutantMetadata(original.syntax, "x > 15", "GreaterThan", original.pos, none))
      ),
      MutantWithId(
        MutantId(1),
        MutatedCode(
          "x <= 15".parseTerm,
          MutantMetadata(original.syntax, "x <= 15", "GreaterThan", original.pos, none)
        )
      )
    )
    val path = Path("foo/bar.scala")
    val instrumenter = new MutantInstrumenter(InstrumenterOptions.testRunner)
    val mutatedFile =
      instrumenter.instrumentFile(SourceContext(source, path), Map(PlaceableTree(original) -> mutants))

    assertNotEquals(mutatedFile.mutatedSourceText[Pure].compile.string, mutatedFile.mutatedSource.text)

    val onDisk = mutatedFile.mutatedSourceText[Pure].compile.string
    val errors = NonEmptyList.of(
      CompilerErrMsg("error0", path.toString, 1, onDisk.indexOf("x > 15").some),
      CompilerErrMsg("error1", path.toString, 1, onDisk.indexOf("x <= 15").some)
    )

    val result = sut.rollbackFiles(errors, Vector(mutatedFile)).value

    assertEquals(result.newFiles, Seq.empty)
    assertEquals(
      result.compileErrors.loneElement._2.map(_.status),
      mutants.map(_ => MutantStatus.CompileError).toVector
    )
  }

  def rollbackableTree = FileUtil.getResourceAsString("rollbackTest/RollbackableTree.scala").map(_.parse[Source].get)
}
