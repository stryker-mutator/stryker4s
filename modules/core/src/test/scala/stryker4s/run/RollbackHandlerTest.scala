package stryker4s.run

import cats.data.{NonEmptyList, NonEmptyVector}
import cats.syntax.option.*
import fansi.Color
import fs2.io.file.Path
import mutationtesting.MutantStatus
import stryker4s.extension.TreeExtensions.FindExtension
import stryker4s.model.*
import stryker4s.mutants.tree.{InstrumenterOptions, MutantInstrumenter}
import stryker4s.testkit.{FileUtil, LogMatchers, Stryker4sIOSuite}

import scala.meta.*

class RollbackHandlerTest extends Stryker4sIOSuite with LogMatchers {

  describe("rollbackFiles") {
    val sut = new RollbackHandler(new MutantInstrumenter(InstrumenterOptions.testRunner))
    test("should remove a non-compiling mutant") {
      rollbackableTree.asserting { tree =>
        val mutantTree = tree.find(q"Files.forall(Paths.get(a))").value
        val mutantMetadata =
          MutantMetadata(mutantTree.syntax, "Files.forall(Paths.get(a))", "MethodExpression", mutantTree.pos)
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
                description = s"L${mutantMetadata.location.start.line}: error2".some
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

    test("should return a Left if no mutants were removed") {
      rollbackableTree.asserting { tree =>
        val mutantTree = tree.find(q"Files.forall(Paths.get(a))").value
        val mutantMetadata =
          MutantMetadata(mutantTree.syntax, "Files.forall(Paths.get(a))", "MethodExpression", mutantTree.pos)

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

    test("should filter out fixed files of the rollbackResult") {
      rollbackableTree.asserting { tree =>
        val mutantTree = tree.find(q"Files.forall(Paths.get(a))").value
        val mutantMetadata =
          MutantMetadata(mutantTree.syntax, "Files.forall(Paths.get(a))", "MethodExpression", mutantTree.pos)
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
                description = s"L${mutantMetadata.location.start.line}: error".some
              )
            )
            .toVector
        )
        assertEquals(result.newFiles, allFiles.tail)
      }
    }
  }

  def rollbackableTree = FileUtil.getResourceAsString("rollbackTest/RollbackableTree.scala").map(_.parse[Source].get)
}
