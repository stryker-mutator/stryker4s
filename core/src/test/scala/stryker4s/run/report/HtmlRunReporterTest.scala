package stryker4s.run.report
import java.util.concurrent.TimeUnit

import better.files.File
import stryker4s.Stryker4sSuite
import stryker4s.config.Config
import stryker4s.extensions.ImplicitMutationConversion._
import stryker4s.extensions.mutationtypes.{GreaterThan, LesserThan}
import stryker4s.model.{Killed, Mutant, MutantRunResults, Survived}

import scala.concurrent.duration.Duration

class HtmlRunReporterTest extends Stryker4sSuite {

  val sut = new HtmlRunReporter
  implicit val config: Config = Config()

  describe("") {
    it("") {
      val mutantRunResult = Killed(0, Mutant(0, GreaterThan, LesserThan), config.baseDir.relativize(File("/core\\src\\main\\scala\\stryker4s\\config.scala")))
      val mutantRunResult2 = Killed(0, Mutant(0, GreaterThan, LesserThan), config.baseDir.relativize(File("/to/a/file")))
      val mutantRunResult3 = Killed(0, Mutant(0, GreaterThan, LesserThan), config.baseDir.relativize(File("/to/a/file2")))
      val mutantRunResult4 = Survived(Mutant(0, GreaterThan, LesserThan), config.baseDir.relativize(File("/to/a/file3")))
      val mutationRunResults = MutantRunResults(List(mutantRunResult, mutantRunResult2, mutantRunResult3, mutantRunResult4), 100.00, Duration(10, TimeUnit.SECONDS))

      sut.report(mutationRunResults)
    }
  }
}
