package stryker4s.run

import cats.data.{NonEmptyList, NonEmptyVector}
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.either.*
import fs2.io.file.{Files, Path}
import mutationtesting.MutantStatus
import stryker4s.config.Config
import stryker4s.exception.InitialTestRunFailedException
import stryker4s.model.*
import stryker4s.testkit.{FileUtil, LogMatchers, Stryker4sIOSuite}
import stryker4s.testutil.TestData
import stryker4s.testutil.stubs.{ReporterStub, RollbackHandlerStub, TestFileResolver, TestRunnerStub}

class MutantRunnerTest extends Stryker4sIOSuite with LogMatchers with TestData {

  val baseDir = FileUtil.getResource("scalaFiles")
  val staticTmpDir = baseDir.resolve("target/stryker4s-tmpDir")

  override def munitFixtures = super.munitFixtures :+ ResourceTestLocalFixture(
    "cleanup staticTmpDir",
    Resource.onFinalize(Files[IO].deleteRecursively(staticTmpDir))
  )

  describe("apply") {

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
        assertLoggedInfo("Setting up mutated environment...")
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
        new MutantRunner(testRunner, fileCollectorStub, rollbackHandler, reporterStub)(staticTmpDirConfig, testLogger)
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

      val testRunner = TestRunnerStub.withResults(initialTestRunResultIsSuccessful = false)()
      val mutant = createMutant.copy(id = MutantId(3))

      val sut =
        new MutantRunner(testRunner, fileCollectorStub, rollbackHandler, reporterStub)(staticTmpDirConfig, testLogger)
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
        new MutantRunner(testRunner, fileCollectorStub, rollbackHandler, reporterStub)(noCleanTmpDirConfig, testLogger)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = NonEmptyVector.one(mutant)
      val mutatedFile = MutatedFile(file, "def foo = 4".parseDef, mutants)

      sut(Vector(mutatedFile)) *>
        Files[IO].exists(staticTmpDir).assertEquals(true)
    }
  }
}
