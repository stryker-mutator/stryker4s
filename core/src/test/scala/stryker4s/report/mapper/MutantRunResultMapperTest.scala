package stryker4s.report.mapper
import java.nio.file.Path

import better.files.File
import mutationtesting._
import org.scalatest.Inside
import stryker4s.config.{Config, Thresholds => ConfigThresholds}
import stryker4s.extension.FileExtensions._
import stryker4s.extension.ImplicitMutationConversion._
import stryker4s.extension.mutationtype._
import stryker4s.model.{Killed, Mutant, Survived}
import stryker4s.scalatest.FileUtil
import stryker4s.testutil.Stryker4sSuite

import scala.meta.{Lit, Term}

class MutantRunResultMapperTest extends Stryker4sSuite with Inside {
  describe("mapper") {
    it("should map 4 files to valid MutationTestReport") {
      val sut = new MutantRunResultMapper {}
      implicit val config: Config = Config(thresholds = ConfigThresholds(high = 60, low = 40))

      val path = FileUtil.getResource("scalaFiles/ExampleClass.scala").relativePath
      val mutantRunResult = Killed(
        toMutant(0, EqualTo, NotEqualTo, path),
        path
      )
      val mutantRunResult2 = Survived(
        toMutant(1, Lit.String("Hugo"), EmptyString, path),
        path
      )
      val path3 = FileUtil.getResource("scalaFiles/simpleFile.scala").relativePath
      val mutantRunResult3 = Killed(
        toMutant(0, GreaterThan, LesserThan, path3),
        path3
      )

      val mutationRunResults = List(mutantRunResult, mutantRunResult2, mutantRunResult3)

      val result = sut.toReport(mutationRunResults)
      inside(result) {
        case MutationTestReport(_, _, thresholds, files) =>
          thresholds should equal(Thresholds(high = 60, low = 40))
          files should have size 2
          val firstResult = files.find(_._1.endsWith("scalaFiles/ExampleClass.scala")).value
          files.find(_._1.endsWith("scalaFiles/simpleFile.scala")).value
          inside(firstResult._2) {
            case MutationTestResult(source, mutants, language) =>
              language should equal("scala")
              mutants should contain only (
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
              source should equal(FileUtil.getResource("scalaFiles/ExampleClass.scala").contentAsString)
          }
      }
    }
  }

  /** Helper method to create a [[stryker4s.model.Mutant]], with the `original` param having the correct `Location` property
    */
  private def toMutant(id: Int, original: Term, category: SubstitutionMutation[_ <: Term], file: Path) = {
    import stryker4s.extension.TreeExtensions.FindExtension

    import scala.meta._
    val parsed = File(file).contentAsString.parse[Source]
    val foundOrig = parsed.get.find(original).value
    Mutant(id, foundOrig, category.tree, category)
  }
}
