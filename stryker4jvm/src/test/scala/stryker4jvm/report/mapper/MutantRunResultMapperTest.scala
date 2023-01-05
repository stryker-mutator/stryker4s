package stryker4jvm.report.mapper

import fs2.io.file.Path
import mutationtesting.*
import org.scalatest.Inside
import stryker4jvm.config.Config
import stryker4jvm.mutator.scala.extensions.mutationtype.{EmptyString, EqualTo, GreaterThan, LesserThan, NotEqualTo, SubstitutionMutation}
import stryker4jvm.reporting.mapper.MutantRunResultMapper
import stryker4jvm.scalatest.FileUtil
import stryker4jvm.testutil.Stryker4jvmSuite

import java.nio.file.Files
import scala.meta.{Lit, Term}

class MutantRunResultMapperTest extends Stryker4jvmSuite with Inside {
  // TODO MutantRunResultMapperTest
//  describe("mapper") {
//    it("should map 4 files to valid MutationTestResult") {
//      val sut = new MutantRunResultMapper {}
//      implicit val config: Config = Config(thresholds = ConfigThresholds(high = 60, low = 40))
//
//      val result = sut.toReport(mutationRunResults)
//
//      result.thresholds should equal(Thresholds(high = 60, low = 40))
//      result.files should have size 2
//      val firstResult = result.files.find(_._1.endsWith("scalaFiles/ExampleClass.scala")).value
//      result.files.find(_._1.endsWith("scalaFiles/simpleFile.scala")).value
//      inside(firstResult._2) { case FileResult(source, mutants, language) =>
//        language should equal("scala")
//        mutants should (
//          contain.only(
//            MutantResult(
//              "0",
//              "EqualityOperator",
//              "!=",
//              Location(Position(4, 27), Position(4, 29)),
//              MutantStatus.Killed
//            ),
//            MutantResult(
//              "1",
//              "StringLiteral",
//              "\"\"",
//              Location(Position(6, 31), Position(6, 37)),
//              MutantStatus.Survived
//            )
//          )
//        )
//        source should equal(
//          new String(Files.readAllBytes(FileUtil.getResource("scalaFiles/ExampleClass.scala").toNioPath))
//        )
//        val framework = result.framework.value
//        result.config.value shouldBe config
//        framework.name shouldBe "Stryker4s"
//        framework.branding.value.homepageUrl shouldBe "https://stryker-mutator.io"
//        framework.branding.value.imageUrl.value should not be empty
//
//        val system = result.system.value
//        system.ci shouldBe sys.env.contains("CI")
//        system.os.value shouldBe OSInformation(platform = sys.props("os.name"), version = Some(sys.props("os.version")))
//        system.cpu.value shouldBe CpuInformation(logicalCores = Runtime.getRuntime().availableProcessors())
//        system.ram.value shouldBe RamInformation(total = Runtime.getRuntime().totalMemory() / 1024 / 1024)
//      }
//
//    }
//
//  }

//  def mutationRunResults = {
//    val path = FileUtil.getResource("scalaFiles/ExampleClass.scala")
//    val mutantRunResult =
//      toMutant(0, EqualTo, NotEqualTo, path).toMutantResult(MutantStatus.Killed)
//
//    val mutantRunResult2 =
//      toMutant(1, Lit.String("Hugo"), EmptyString, path).toMutantResult(MutantStatus.Survived)
//
//    val path3 = FileUtil.getResource("scalaFiles/simpleFile.scala")
//    val mutantRunResult3 =
//      toMutant(0, GreaterThan, LesserThan, path3).toMutantResult(MutantStatus.Killed)
//
//    Map(path -> Vector(mutantRunResult, mutantRunResult2), path3 -> Vector(mutantRunResult3))
//  }

  /** Helper method to create a [[stryker4s.model.MutantWithId]], with the `original` param having the correct
    * `Location` property
    */
//  private def toMutant(id: Int, original: Term, category: SubstitutionMutation[? <: Term], file: Path) = {
//    import scala.meta.*
//    val parsed = file.toNioPath.parse[Source]
//    val foundOrig = parsed.get.find(original).value
//    MutantWithId(
//      MutantId(id),
//      MutatedCode(
//        foundOrig,
//        MutantMetadata(foundOrig.syntax, category.tree.syntax, category.mutationName, foundOrig.pos)
//      )
//    )
//  }
}
