package stryker4s.report.mapper
import java.nio.file.{Path, Paths}

import better.files.File
import org.scalatest.Inside
import stryker4s.config.{Config, Thresholds => ConfigThresholds}
import stryker4s.extension.ImplicitMutationConversion._
import stryker4s.extension.mutationtype.{EmptyString, False, Mutation, True}
import stryker4s.model.{Killed, Mutant, MutantRunResults, Survived}
import stryker4s.report.model._
import stryker4s.testutil.Stryker4sSuite

import scala.concurrent.duration._
import scala.meta.{Lit, Term, Tree}

class MutantRunResultMapperTest extends Stryker4sSuite with Inside {
  describe("mapper") {
    it("should map 4 files to valid MutationTestReport") {
      val sut = new MutantRunResultMapper {}
      implicit val config: Config = Config(thresholds = ConfigThresholds(high = 60, low = 40))

      val path = Paths.get("core/src/main/scala/stryker4s/extension/mutationtype/BooleanLiteral.scala")
      val mutantRunResult = Killed(
        toMutant(0, True, False, True, path),
        path
      )
      val mutantRunResult2 = Survived(
        toMutant(1, False, True, False, path),
        path
      )
      val path3 = Paths.get("core/src/main/scala/stryker4s/report/ConsoleReporter.scala")
      val mutantRunResult3 = Killed(
        toMutant(0, Lit.String("Mutation score dangerously low!"), EmptyString, EmptyString, path3),
        path3
      )
      val path4 = Paths.get("core/src/main/scala/stryker4s/report/mapper/MutantRunResultMapper.scala")
      val mutantRunResult4 = Survived(
        toMutant(0, Lit.String("1"), EmptyString, EmptyString, path4),
        path4
      )

      val mutationRunResults =
        MutantRunResults(List(mutantRunResult, mutantRunResult2, mutantRunResult3, mutantRunResult4), 100.0, 10.seconds)

      val result: MutationTestReport = sut.toReport(mutationRunResults)
      inside(result) {
        case MutationTestReport(schemaVersion, thresholds, files) =>
          schemaVersion shouldBe "1"
          thresholds should equal(Thresholds(high = 60, low = 40))
          inside(files("core/src/main/scala/stryker4s/extension/mutationtype/BooleanLiteral.scala")) {
            case MutationTestResult(source, mutants, language) =>
              language should equal("scala")
              mutants should contain only (
                MutantResult("0",
                             "BooleanLiteral",
                             "false",
                             Location(Position(6, 48), Position(6, 52)),
                             MutantStatus.Killed),
                MutantResult("1",
                             "BooleanLiteral",
                             "true",
                             Location(Position(10, 48), Position(10, 53)),
                             MutantStatus.Survived)
              )
              source should equal(
                File("core/src/main/scala/stryker4s/extension/mutationtype/BooleanLiteral.scala").contentAsString)
          }
      }
    }
  }

  private def toMutant(id: Int, original: Term, to: Term, category: Mutation[_ <: Tree], file: Path): Mutant = {
    import stryker4s.extension.TreeExtensions.FindExtension
    import scala.meta._
    val parsed = File(file).contentAsString.parse[Source]
    val foundOrig = parsed.get.find(original).value
    Mutant(id, foundOrig, to, category)
  }
}
