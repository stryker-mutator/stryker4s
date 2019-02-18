package stryker4s

import java.nio.file.Paths

import org.mockito.MockitoSugar
import org.mockito.captor.ArgCaptor
import org.scalatest.Inside
import stryker4s.config.Config
import stryker4s.model.{Killed, Mutant, MutantRunResults}
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.{ActiveMutationContext, MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{FileCollector, MutantFinder, MutantMatcher}
import stryker4s.run.process.{Command, ProcessMutantRunner}
import stryker4s.run.report.MutantRunReporter
import stryker4s.run.threshold.SuccessStatus
import stryker4s.scalatest.{FileUtil, LogMatchers}
import stryker4s.testutil.Stryker4sSuite
import stryker4s.testutil.stubs.{TestProcessRunner, TestSourceCollector}

import scala.collection.Iterable
import scala.util.Success

class Stryker4sTest extends Stryker4sSuite with LogMatchers with MockitoSugar with Inside {

  describe("run") {
    it("should call mutate files and report the results") {
      implicit val conf: Config = Config(baseDir = FileUtil.getResource("scalaFiles"))
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val testFiles = Seq(file)
      val testSourceCollector = new TestSourceCollector(testFiles)
      val testProcessRunner = TestProcessRunner(Success(1), Success(1), Success(1), Success(1))
      val testMutantRunner = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, new FileCollector())
      val testReporter = mock[MutantRunReporter]

      val sut = new Stryker4s(
        testSourceCollector,
        new Mutator(new MutantFinder(new MutantMatcher),
                    new StatementTransformer,
                    new MatchBuilder(ActiveMutationContext.sysProps)),
        testMutantRunner,
        testReporter
      )

      val result = sut.run()

      val captor = ArgCaptor[MutantRunResults]
      verify(testReporter).reportFinishedRun(captor)
      val reportedResults = captor.value.results

      val expectedPath = Paths.get("simpleFile.scala")

      result shouldBe SuccessStatus
      reportedResults should matchPattern {
        case Seq(Killed(Mutant(0, _, _, _), `expectedPath`),
                  Killed(Mutant(1, _, _, _), `expectedPath`),
                  Killed(Mutant(2, _, _, _), `expectedPath`),
                  Killed(Mutant(3, _, _, _), `expectedPath`)) =>
      }
    }

    it("should log a warning when JVM max memory is too low") {
      implicit val conf: Config = Config()
      val testSourceCollector = new TestSourceCollector(Seq())
      val testProcessRunner = TestProcessRunner()
      val testMutantRunner = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, testSourceCollector)
      val testReporter = mock[MutantRunReporter]

      val sut: Stryker4s =
        new Stryker4s(
          testSourceCollector,
          new Mutator(new MutantFinder(new MutantMatcher),
                      new StatementTransformer,
                      new MatchBuilder(ActiveMutationContext.sysProps)),
          testMutantRunner,
          testReporter
        ) {

          override def jvmMemory2GBOrHigher: Boolean = false
        }

      sut.run()

      "The JVM has less than 2GB memory available. We advise increasing this to 4GB when running Stryker4s." shouldBe loggedAsWarning
      "This can be done in sbt by setting an environment variable: SBT_OPTS=\"-XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=4G -Xmx4G\" " shouldBe loggedAsWarning
      "Visit https://github.com/stryker-mutator/stryker4s#memory-usage for more info." shouldBe loggedAsWarning
    }

    it("should not log a warning when JVM max memory is high enough") {
      implicit val conf: Config = Config()
      val testSourceCollector = new TestSourceCollector(Seq())
      val testProcessRunner = TestProcessRunner()
      val testMutantRunner = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, testSourceCollector)
      val testReporter = mock[MutantRunReporter]

      val sut: Stryker4s =
        new Stryker4s(
          testSourceCollector,
          new Mutator(new MutantFinder(new MutantMatcher),
                      new StatementTransformer,
                      new MatchBuilder(ActiveMutationContext.sysProps)),
          testMutantRunner,
          testReporter
        ) {

          override def jvmMemory2GBOrHigher: Boolean = true
        }

      sut.run()

      "The JVM has less than 2GB memory available. We advise increasing this to 4GB when running Stryker4s." should not be loggedAsWarning
      "This can be done in sbt by setting an environment variable: SBT_OPTS=\"-XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=4G -Xmx4G\" " should not be loggedAsWarning
      "Visit https://github.com/stryker-mutator/stryker4s#memory-usage for more info." should not be loggedAsWarning
    }
  }
}
