package stryker4s.run

import cats.data.{NonEmptyList, NonEmptyVector}
import cats.effect.{IO, Resource}
import cats.syntax.either.*
import fansi.Color
import fs2.io.file.{Files, Path}
import mutationtesting.{MutantResult, MutantStatus}
import stryker4s.config.Config
import stryker4s.exception.InitialTestRunFailedException
import stryker4s.model.*
import stryker4s.testkit.{FileUtil, LogMatchers, Stryker4sIOSuite}
import stryker4s.testrunner.api.{CoverageReport, TestFile}
import stryker4s.testutil.TestData
import stryker4s.testutil.stubs.{ReporterStub, RollbackHandlerStub, TestFileResolver, TestRunnerStub}

import scala.concurrent.duration.*

class MutantRunnerTest extends Stryker4sIOSuite with LogMatchers with TestData {

  val baseDir = FileUtil.getResource("scalaFiles")
  val staticTmpDir = baseDir.resolve("target/stryker4s-tmpDir")

  override def munitFixtures = super.munitFixtures :+ ResourceTestLocalFixture(
    "cleanup staticTmpDir",
    Resource.onFinalize(Files[IO].exists(staticTmpDir).ifM(Files[IO].deleteRecursively(staticTmpDir), IO.unit))
  )

  implicit val config: Config = Config.default.copy(baseDir = baseDir)

  val staticTmpDirConfig = config.copy(staticTmpDir = true)
  val noCleanTmpDirConfig = staticTmpDirConfig.copy(cleanTmpDir = false)

  test("should return a mutationScore of 66.67 when 2 of 3 mutants are killed") {
    val fileCollectorStub = new TestFileResolver(Seq.empty)
    val reporterStub = ReporterStub()
    val rollbackHandler = RollbackHandlerStub.alwaysSuccessful()
    val mutant = createMutant.copy(id = MutantId(3))
    val secondMutant = createMutant.copy(id = MutantId(1))
    val thirdMutant = createMutant.copy(id = MutantId(2))

    val testRunner = { (path: Path) =>
      // Static temp dir is not used with default settings.
      assert(!path.endsWith("stryker4s-tmpDir"))
      TestRunnerStub.withResults(
        mutant.toMutantResult(MutantStatus.Killed),
        secondMutant.toMutantResult(MutantStatus.Killed),
        thirdMutant.toMutantResult(MutantStatus.Survived)
      )(path)
    }

    val sut = new MutantRunner(testRunner, fileCollectorStub, rollbackHandler, reporterStub)
    val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
    val mutants = NonEmptyVector.of(mutant, secondMutant, thirdMutant)
    val mutatedFile = MutatedFile(file, "def foo = 4".parseDef, mutants)
    sut(Vector(mutatedFile)).asserting { case RunResult(results, _, _) =>
      assertLoggedInfo("Setting up environment for 1 mutated file(s)...")
      assertLoggedDebug(s"Using temp directory: ")
      assertLoggedDebug(s"Writing ${results.loneElement._1} file to ")
      assertLoggedInfo("Starting initial test run...")
      assertLoggedInfo("Initial test run succeeded! Testing mutants...")
      val (path, resultForFile) = results.loneElement
      assertEquals(path, file)
      assertEquals(
        resultForFile.map(_.status),
        Vector(MutantStatus.Killed, MutantStatus.Survived, MutantStatus.Killed)
      )
    }
  }

  test("should return a mutationScore of 66.67 when 2 of 3 mutants are killed and 1 doesn't compile.") {
    val mutant = createMutant.copy(id = MutantId(3))
    val secondMutant = createMutant.copy(id = MutantId(1))
    val thirdMutant = createMutant.copy(id = MutantId(2))
    val compileErrorResult = thirdMutant.toMutantResult(MutantStatus.CompileError)
    val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
    val mutants = NonEmptyVector.of(mutant, secondMutant, thirdMutant)
    val mutatedFile = MutatedFile(file, "def foo = 4".parseDef, mutants)

    val fileCollectorStub = new TestFileResolver(Seq.empty)
    val reporterStub = ReporterStub()

    val rollbackHandler = RollbackHandlerStub.withResult(
      RollbackResult(
        Vector(mutatedFile.copy(mutants = NonEmptyVector.of(mutant, secondMutant))),
        Map(file -> Vector(compileErrorResult))
      ).asRight
    )

    val testRunner =
      TestRunnerStub.withInitialCompilerError(
        NonEmptyList.one(CompilerErrMsg("blah", "scalaFiles/simpleFile.scala", 123)),
        mutant.toMutantResult(MutantStatus.Killed),
        secondMutant.toMutantResult(MutantStatus.Killed)
      )

    val sut = new MutantRunner(testRunner, fileCollectorStub, rollbackHandler, reporterStub)

    sut(Vector(mutatedFile)).asserting { case RunResult(results, _, _) =>
      val (path, resultForFile) = results.loneElement
      assertEquals(path, file)
      assertLoggedInfo("Attempting to remove 1 mutant(s) that gave a compile error...")
      assertEquals(
        resultForFile.map(_.status),
        Vector(MutantStatus.Killed, MutantStatus.Killed, MutantStatus.CompileError)
      )
    }
  }

  test("should use static temp dir if it was requested") {
    val fileCollectorStub = new TestFileResolver(Seq.empty)
    val reporterStub = ReporterStub()
    val rollbackHandler = RollbackHandlerStub.alwaysSuccessful()
    val mutant = createMutant.copy(id = MutantId(3))

    val testRunner = { (path: Path) =>
      // Static temp dir is used.
      assert(path.endsWith("stryker4s-tmpDir"))
      TestRunnerStub.withResults(mutant.toMutantResult(MutantStatus.Killed))(path)
    }

    val sut =
      new MutantRunner(testRunner, fileCollectorStub, rollbackHandler, reporterStub)(using
        staticTmpDirConfig,
        testLogger
      )
    val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
    val mutants = NonEmptyVector.one(mutant)
    val mutatedFile = MutatedFile(file, "def foo = 4".parseDef, mutants)

    sut(Vector(mutatedFile)) *>
      // Cleaned up after run
      Files[IO].exists(staticTmpDir).assertEquals(false)
  }

  test("should not clean up tmp dir on errors") {
    val fileCollectorStub = new TestFileResolver(Seq.empty)
    val reporterStub = ReporterStub()
    val rollbackHandler = RollbackHandlerStub.alwaysSuccessful()

    val testRunner = TestRunnerStub.withResults(NoCoverageInitialTestRun(false))()
    val mutant = createMutant.copy(id = MutantId(3))

    val sut =
      new MutantRunner(testRunner, fileCollectorStub, rollbackHandler, reporterStub)(using
        staticTmpDirConfig,
        testLogger
      )
    val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
    val mutants = NonEmptyVector.one(mutant)
    val mutatedFile = MutatedFile(file, "def foo = 4".parseDef, mutants)

    sut(Vector(mutatedFile))
      .interceptMessage[InitialTestRunFailedException](
        "Initial test run failed. Please make sure your tests pass before running Stryker4s."
      )
  }

  test("should not clean up tmp dir if clean-tmp-dir is disabled") {
    val fileCollectorStub = new TestFileResolver(Seq.empty)
    val reporterStub = ReporterStub()
    val rollbackHandler = RollbackHandlerStub.alwaysSuccessful()
    val mutant = createMutant.copy(id = MutantId(1))

    val testRunner = TestRunnerStub.withResults(mutant.toMutantResult(MutantStatus.Killed))

    val sut =
      new MutantRunner(testRunner, fileCollectorStub, rollbackHandler, reporterStub)(using
        noCleanTmpDirConfig,
        testLogger
      )
    val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
    val mutants = NonEmptyVector.one(mutant)
    val mutatedFile = MutatedFile(file, "def foo = 4".parseDef, mutants)

    sut(Vector(mutatedFile)) *>
      Files[IO]
        .exists(staticTmpDir)
        .assert
        .assertLoggedInfo(
          s"Not deleting $staticTmpDir (turn off cleanTmpDir to disable this). Please clean it up manually."
        )
  }

  test("should debug log timing when a mutant is tested") {
    val fileCollectorStub = new TestFileResolver(Seq.empty)
    val reporterStub = ReporterStub()
    val rollbackHandler = RollbackHandlerStub.alwaysSuccessful()
    val mutant = createMutant.copy(id = MutantId(1))

    val testRunner = TestRunnerStub.withResults(mutant.toMutantResult(MutantStatus.Killed))
    val sut = new MutantRunner(testRunner, fileCollectorStub, rollbackHandler, reporterStub)
    val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
    val mutants = NonEmptyVector.one(mutant)
    val mutatedFile = MutatedFile(file, "def foo = 4".parseDef, mutants)

    sut(Vector(mutatedFile))
      .assertLoggedDebug(s"Running mutant $mutant")
      .assertLoggedDebug("Mutant 1 tested in")
  }

  test("should warn when all mutants have no code coverage") {
    val fileCollectorStub = new TestFileResolver(Seq.empty)
    val reporterStub = ReporterStub()
    val rollbackHandler = RollbackHandlerStub.alwaysSuccessful()
    val mutant = createMutant.copy(id = MutantId(1))

    val allNoCoverageTestRunner: Path => Either[NonEmptyList[CompilerErrMsg], Resource[IO, TestRunnerPool]] =
      (_: Path) =>
        ResourcePool(
          NonEmptyList.one(
            Resource.pure[IO, TestRunner](new TestRunner {
              override def initialTestRun(): IO[InitialTestRunResult] =
                IO.pure(
                  InitialTestRunCoverageReport(
                    isSuccessful = true,
                    firstRun = CoverageReport.empty,
                    secondRun = CoverageReport.empty,
                    duration = 100.milliseconds,
                    testNames = Seq.empty
                  )
                )
              override def runMutant(mutant: MutantWithId, testNames: Seq[TestFile]): IO[MutantResult] =
                IO.raiseError(new Exception("runMutant should not be called when all mutants are NoCoverage"))
            })
          )
        ).asRight

    val sut = new MutantRunner(allNoCoverageTestRunner, fileCollectorStub, rollbackHandler, reporterStub)
    val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
    val mutants = NonEmptyVector.one(mutant)
    val mutatedFile = MutatedFile(file, "def foo = 4".parseDef, mutants)

    sut(Vector(mutatedFile)).asserting { results =>
      val mutantResult = results.results.loneElement._2.loneElement
      assertEquals(mutantResult.status, MutantStatus.NoCoverage)
      assertEquals(mutantResult.statusReason.value, "This mutant was not covered by any code.")
      assertLoggedInfo(s"1 mutant(s) detected as having no code coverage. They will be skipped and marked as ${Color
          .Magenta("NoCoverage")}")
      assertLoggedDebug("NoCoverage mutant ids are: 1")
      assertLoggedWarn(
        "All mutants have no code coverage and will not be tested. This typically means that the test runner did not collect any coverage information."
      )
      assertLoggedWarn(
        "You can enable 'log-test-runner-stdout' in your configuration to see test runner output and diagnose the issue. See https://stryker-mutator.io/docs/stryker4s/configuration/ for more information."
      )
      assertNotLoggedDebug("Static mutant ids are:")
    }
  }

  test("should log and report static mutants") {
    val fileCollectorStub = new TestFileResolver(Seq.empty)
    val reporterStub = ReporterStub()
    val rollbackHandler = RollbackHandlerStub.alwaysSuccessful()
    val mutant = createMutant.copy(id = MutantId(1))

    val testRunner = TestRunnerStub.withResults(
      InitialTestRunCoverageReport(
        isSuccessful = true,
        firstRun = CoverageReport(Map(mutant.id -> Seq.empty)),
        secondRun = CoverageReport.empty,
        duration = 100.milliseconds,
        testNames = Seq.empty
      )
    )(mutant.toMutantResult(MutantStatus.NoCoverage))
    val sut = new MutantRunner(testRunner, fileCollectorStub, rollbackHandler, reporterStub)
    val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
    val mutants = NonEmptyVector.one(mutant)
    val mutatedFile = MutatedFile(file, "def foo = 4".parseDef, mutants)

    sut(Vector(mutatedFile)).asserting { case RunResult(results, _, _) =>
      val mutantResult = results.loneElement._2.loneElement
      assertEquals(mutantResult.status, MutantStatus.Ignored)
      assertEquals(
        mutantResult.statusReason.value,
        "This is a 'static' mutant and can not be tested. If you still want to have this mutant tested, change your code to make this value initialize each time it is called."
      )
      assert(mutantResult.static.value)
      assertLoggedInfo(
        s"1 mutant(s) detected as static. They will be skipped and marked as ${Color.Magenta("Ignored")}"
      )
      assertLoggedDebug("Static mutant ids are: 1")
      assertNotLoggedDebug("NoCoverage mutant ids are:")
    }
  }
}
