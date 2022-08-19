package stryker4s.report.mapper

import fs2.io.file.Path
import mutationtesting.*
import org.scalatest.Inside
import stryker4s.config.{Config, Thresholds as ConfigThresholds}
import stryker4s.extension.FileExtensions.*
import stryker4s.extension.ImplicitMutationConversion.*
import stryker4s.extension.mutationtype.*
import stryker4s.model.{Killed, Mutant, MutantId, Survived}
import stryker4s.scalatest.FileUtil
import stryker4s.testutil.Stryker4sSuite

import java.nio.file.Files
import scala.meta.{Lit, Term}

class MutantRunResultMapperTest extends Stryker4sSuite with Inside {
  describe("mapper") {
    it("should map 4 files to valid MutationTestResult") {
      val sut = new MutantRunResultMapper {}
      implicit val config: Config = Config(thresholds = ConfigThresholds(high = 60, low = 40))

      val result = sut.toReport(mutationRunResults)
      result.thresholds should equal(Thresholds(high = 60, low = 40))
      result.files should have size 2
      val firstResult = result.files.find(_._1.endsWith("scalaFiles/ExampleClass.scala")).value
      result.files.find(_._1.endsWith("scalaFiles/simpleFile.scala")).value
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
        val framework = result.framework.value
        result.config.value shouldBe config
        framework.name shouldBe "Stryker4s"
        framework.branding.value.homepageUrl shouldBe "https://stryker-mutator.io"
        framework.branding.value.imageUrl.value should not be empty

        val system = result.system.value
        system.ci shouldBe sys.env.contains("CI")
        system.os.value shouldBe OSInformation(platform = sys.props("os.name"), version = Some(sys.props("os.version")))
        system.cpu.value shouldBe CpuInformation(logicalCores = Runtime.getRuntime().availableProcessors())
        system.ram.value shouldBe RamInformation(total = Runtime.getRuntime().totalMemory())
      }

    }

  }

  def mutationRunResults(implicit config: Config) = {
    val path = FileUtil.getResource("scalaFiles/ExampleClass.scala").relativePath
    val mutantRunResult = Killed(
      toMutant(0, EqualTo, NotEqualTo, path)
    )
    val mutantRunResult2 = Survived(
      toMutant(1, Lit.String("Hugo"), EmptyString, path)
    )
    val path3 = FileUtil.getResource("scalaFiles/simpleFile.scala").relativePath
    val mutantRunResult3 = Killed(
      toMutant(0, GreaterThan, LesserThan, path3)
    )

    Map(path -> List(mutantRunResult, mutantRunResult2), path3 -> List(mutantRunResult3))
  }

  /** Helper method to create a [[stryker4s.model.Mutant]], with the `original` param having the correct `Location`
    * property
    */
  private def toMutant(id: Int, original: Term, category: SubstitutionMutation[? <: Term], file: Path) = {
    import stryker4s.extension.TreeExtensions.FindExtension

    import scala.meta.*
    val parsed = file.toNioPath.parse[Source]
    val foundOrig = parsed.get.find(original).value
    Mutant(MutantId(id), foundOrig, category.tree, category)
  }
}
