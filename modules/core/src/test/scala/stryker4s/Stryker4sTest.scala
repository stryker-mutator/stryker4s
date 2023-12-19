package stryker4s

import stryker4s.config.Config
import stryker4s.files.ConfigFilesResolver
import stryker4s.mutants.applymutants.ActiveMutationContext
import stryker4s.mutants.findmutants.{MutantFinder, MutantMatcherImpl}
import stryker4s.mutants.tree.{InstrumenterOptions, MutantCollector, MutantInstrumenter}
import stryker4s.mutants.{Mutator, TreeTraverserImpl}
import stryker4s.run.MutantRunner
import stryker4s.run.threshold.SuccessStatus
import stryker4s.testkit.{FileUtil, LogMatchers, Stryker4sIOSuite}
import stryker4s.testutil.stubs.{ReporterStub, RollbackHandlerStub, TestFileResolver, TestProcessRunner, TestRunnerStub}

import scala.util.Success

class Stryker4sTest extends Stryker4sIOSuite with LogMatchers {

  test("should call mutate files and report the results") {
    val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
    val testFiles = Seq(file)
    val testSourceCollector = new TestFileResolver(testFiles)
    val testProcessRunner = TestProcessRunner(Success(1), Success(1), Success(1), Success(1))
    val reporterStub = ReporterStub()
    val rollbackHandler = RollbackHandlerStub.alwaysSuccessful()

    implicit val conf: Config = Config.default.copy(baseDir = FileUtil.getResource("scalaFiles"))

    val testMutantRunner =
      new MutantRunner(
        TestRunnerStub.resource,
        new ConfigFilesResolver(testProcessRunner),
        rollbackHandler,
        reporterStub
      )

    val sut = new Stryker4s(
      testSourceCollector,
      new Mutator(
        new MutantFinder(),
        new MutantCollector(new TreeTraverserImpl(), new MutantMatcherImpl()),
        new MutantInstrumenter(InstrumenterOptions.sysContext(ActiveMutationContext.sysProps))
      ),
      testMutantRunner,
      reporterStub
    )

    sut.run().assertEquals(SuccessStatus) *>
      reporterStub.onRunFinishedCalls.asserting { case runReports =>
        val reportedResults = runReports.loneElement.report

        assertEquals(reportedResults.files.flatMap(_._2.mutants).size, 4)
        assertEquals(reportedResults.files.loneElement._1, "simpleFile.scala")
      }

  }
}
