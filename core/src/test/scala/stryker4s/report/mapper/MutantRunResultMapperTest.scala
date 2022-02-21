package stryker4s.report.mapper

import fs2.io.file.Path
import mutationtesting.*
import org.scalatest.Inside
import stryker4s.config.{Config, Thresholds as ConfigThresholds}
import stryker4s.extension.FileExtensions.*
import stryker4s.extension.ImplicitMutationConversion.*
import stryker4s.extension.mutationtype.*
import stryker4s.model.{MutantId, MutantMetadata, MutantWithId, MutatedCode}
import stryker4s.scalatest.FileUtil
import stryker4s.testutil.Stryker4sSuite

import java.nio.file.Files
import scala.meta.{Lit, Term}

class MutantRunResultMapperTest extends Stryker4sSuite with Inside {
  describe("mapper") {
    it("should map 4 files to valid MutationTestResult") {
      val sut = new MutantRunResultMapper {}
      implicit val config: Config = Config(thresholds = ConfigThresholds(high = 60, low = 40))

      val path = FileUtil.getResource("scalaFiles/ExampleClass.scala").relativePath
      val mutantRunResult =
        toMutant(0, EqualTo, NotEqualTo, path).toMutantResult(MutantStatus.Killed)

      val mutantRunResult2 =
        toMutant(1, Lit.String("Hugo"), EmptyString, path).toMutantResult(MutantStatus.Survived)

      val path3 = FileUtil.getResource("scalaFiles/simpleFile.scala").relativePath
      val mutantRunResult3 =
        toMutant(0, GreaterThan, LesserThan, path3).toMutantResult(MutantStatus.Killed)

      val mutationRunResults = Map(path -> Vector(mutantRunResult, mutantRunResult2), path3 -> Vector(mutantRunResult3))

      val result = sut.toReport(mutationRunResults)
      inside(result) { case m: MutationTestResult[Config] =>
        m.thresholds should equal(Thresholds(high = 60, low = 40))
        m.files should have size 2
        val firstResult = m.files.find(_._1.endsWith("scalaFiles/ExampleClass.scala")).value
        m.files.find(_._1.endsWith("scalaFiles/simpleFile.scala")).value
        inside(firstResult._2) { case FileResult(source, mutants, language) =>
          language should equal("scala")
          mutants should (
            contain.only(
              MutantResult(
                "0",
                "EqualityOperator",
                "!=",
                Location(Position(4, 27), Position(4, 29)),
                MutantStatus.Killed
              ),
              MutantResult(
                "1",
                "StringLiteral",
                "\"\"",
                Location(Position(6, 31), Position(6, 37)),
                MutantStatus.Survived
              )
            )
          )
          source should equal(
            new String(Files.readAllBytes(FileUtil.getResource("scalaFiles/ExampleClass.scala").toNioPath))
          )
          m.config.value shouldBe config
        }
      }
    }
  }

  /** Helper method to create a [[stryker4s.model.Mutant]], with the `original` param having the correct `Location`
    * property
    */
  private def toMutant(id: Int, original: Term, category: SubstitutionMutation[? <: Term], file: Path) = {
    import stryker4s.extension.TreeExtensions.FindExtension

    import scala.meta.*
    val parsed = file.toNioPath.parse[Source]
    val foundOrig = parsed.get.find(original).value
    MutantWithId(
      MutantId(id),
      MutatedCode(
        foundOrig,
        MutantMetadata(foundOrig.syntax, category.tree.syntax, category.mutationName, foundOrig.pos)
      )
    )
  }
}
