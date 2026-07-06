package stryker4s.run

import cats.data.{EitherT, NonEmptyList}
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fansi.Color
import fs2.io.file.{Files, Path}
import fs2.{Pipe, Stream}
import mutationtesting.{MutantResult, MutantStatus, TestDefinition, TestFile, TestFileDefinitionDictionary}
import stryker4s.config.Config
import stryker4s.exception.{InitialTestRunFailedException, UnableToFixCompilerErrorsException}
import stryker4s.extension.DurationExtensions.*
import stryker4s.extension.FileExtensions.*
import stryker4s.files.FileResolver
import stryker4s.log.Logger
import stryker4s.model.*
import stryker4s.report.{MutantTestedEvent, Reporter}

import java.nio
import scala.collection.immutable.SortedMap

class MutantRunner(
    createTestRunnerPool: Path => Either[NonEmptyList[CompilerErrMsg], Resource[IO, TestRunnerPool]],
    fileResolver: FileResolver,
    rollbackHandler: RollbackHandler,
    reporter: Reporter
)(implicit config: Config, log: Logger) {

  def apply(mutatedFiles: Vector[MutatedFile]): IO[RunResult] = {

    val withRollback = handleRollback(mutatedFiles)

    withRollback

  }

  def handleRollback(mutatedFiles: Vector[MutatedFile]) =
    EitherT(run(mutatedFiles))
      .leftFlatMap { errors =>
        log.info(s"Attempting to remove ${errors.size} mutant(s) that gave a compile error...")
        // Retry once with the non-compiling mutants removed
        EitherT(
          rollbackHandler
            .rollbackFiles(errors, mutatedFiles)
            .flatTraverse { case RollbackResult(newFiles, rollbackedMutants) =>
              run(newFiles).map { result =>
                result.map { r =>
                  // Combine the results of the run with the results of the rollbacked mutants
                  r.copy(results = r.results.alignCombine(rollbackedMutants))
                }
              }
            }
        )
      }
      // Failed at removing the non-compiling mutants
      .leftMap(UnableToFixCompilerErrorsException(_))
      .rethrowT

  def run(mutatedFiles: Seq[MutatedFile]): IO[Either[NonEmptyList[CompilerErrMsg], RunResult]] = {
    prepareEnv(mutatedFiles).use { path =>
      createTestRunnerPool(path).traverse {
        _.use { testRunnerPool =>
          for {
            initialTestRunResult <- testRunnerPool.loan.use(testrunner => initialTestRun(testrunner))
            testFiles = createTestFileDictionary(initialTestRunResult)
            t <- runMutants(mutatedFiles, testRunnerPool, initialTestRunResult).timed
            (duration, results) = t
          } yield RunResult(results, testFiles, duration)
        }
      }
    }
  }

  private def createTestFileDictionary(initialTest: InitialTestRunResult): Option[TestFileDefinitionDictionary] = {
    if (initialTest.testNames.isEmpty) none
    else
      initialTest.testNames
        .map(testFile =>
          testFile.fullyQualifiedName.replace(".", "/") + ".scala" -> TestFile(
            testFile.definitions.map(s => TestDefinition(id = s.id.toString, s.name, none))
          )
        )
        .toMap
        .some
  }

  def prepareEnv(mutatedFiles: Seq[MutatedFile]): Resource[IO, Path] = {
    val targetDir = config.baseDir / "target"
    for {
      _ <- Files[IO].createDirectories(targetDir).toResource
      tmpDir <- prepareTmpDir(targetDir)
      _ <- setupFiles(tmpDir, mutatedFiles.toSeq).toResource
    } yield tmpDir
  }

  private def prepareTmpDir(targetDir: Path): Resource[IO, Path] = {
    val tmpDirCreated = if (config.staticTmpDir) {
      val staticTmpDir = targetDir / "stryker4s-tmpDir"
      Files[IO].createDirectory(staticTmpDir).as(staticTmpDir)
    } else {
      Files[IO].createTempDirectory(targetDir.some, "stryker4s-", none)
    }
    Resource.makeCase(tmpDirCreated)(tmpDirFinalizeCase)
  }

  private def tmpDirFinalizeCase: (Path, Resource.ExitCase) => IO[Unit] = {
    case (tmpDir, Resource.ExitCase.Succeeded | Resource.ExitCase.Canceled) =>
      if (config.cleanTmpDir) {
        Files[IO].deleteRecursively(tmpDir)
      } else {
        IO(log.info(s"Not deleting $tmpDir (turn off cleanTmpDir to disable this). Please clean it up manually."))
      }
    case (tmpDir, _: Resource.ExitCase.Errored) =>
      // Enable the user do some manual actions before she retries.
      IO(log.warn(s"Not deleting $tmpDir after error. Please clean it up manually."))
  }

  private def setupFiles(tmpDir: Path, mutatedFiles: Seq[MutatedFile]): IO[Unit] =
    IO(log.info(s"Setting up environment for ${mutatedFiles.size} mutated file(s)...")) *>
      IO(log.debug("Using temp directory: " + tmpDir)) *> {
        val mutatedPaths = mutatedFiles.map(_.fileOrigin)
        val unmutatedFilesStream =
          fileResolver.files
            .filterNot(mutatedPaths.contains)
            .through(writeOriginalFile(tmpDir))

        val mutatedFilesStream = Stream
          .emits(mutatedFiles)
          .through(writeMutatedFile(tmpDir))
        (unmutatedFilesStream merge mutatedFilesStream).compile.drain
      }

  def writeOriginalFile(tmpDir: Path): Pipe[IO, Path, Unit] =
    in =>
      in.parEvalMapUnordered(Config.cpuParallelism) { file =>
        val newSubPath = file.inSubDir(tmpDir)

        IO(log.debug(s"Copying $file to $newSubPath")) *>
          Files[IO].createDirectories(newSubPath.parent.get) *>
          Files[IO].copy(file, newSubPath).void
      }

  def writeMutatedFile(tmpDir: Path): Pipe[IO, MutatedFile, Unit] =
    _.parEvalMapUnordered(Config.cpuParallelism) { mutatedFile =>
      val targetPath = mutatedFile.fileOrigin.inSubDir(tmpDir)
      IO(log.debug(s"Writing ${mutatedFile.fileOrigin} file to $targetPath")) *>
        Files[IO].createDirectories(targetPath.parent.get) *>
        Stream
          .eval(IO(mutatedFile.mutatedSource.text))
          .through(Files[IO].writeUtf8(targetPath))
          .compile
          .drain
    }

  private def runMutants(
      mutatedFiles: Seq[MutatedFile],
      testRunnerPool: TestRunnerPool,
      coverageExclusions: InitialTestRunResult
  ): IO[MutantResultsPerFile] = {
    val allMutants = mutatedFiles.flatMap(m => m.mutants.toVector.map(m.fileOrigin -> _))

    val (staticMutants, rest) = allMutants.partition(m => coverageExclusions.staticMutants.contains(m._2.id))

    val (noCoverageMutants, testableMutants) =
      rest.partition(m => coverageExclusions.hasCoverage && !coverageExclusions.coveredMutants.contains(m._2.id))

    if (noCoverageMutants.nonEmpty) {
      log.info(
        s"${noCoverageMutants.size} mutant(s) detected as having no code coverage. They will be skipped and marked as ${Color
            .Magenta("NoCoverage")}"
      )
      log.debug(s"NoCoverage mutant ids are: ${noCoverageMutants.map(_._2.id).mkString(", ")}")
      if (testableMutants.isEmpty && staticMutants.isEmpty) {
        log.warn(
          "All mutants have no code coverage and will not be tested. This typically means that the test runner did not collect any coverage information."
        )
        log.warn(
          "You can enable 'log-test-runner-stdout' in your configuration to see test runner output and diagnose the issue. See https://stryker-mutator.io/docs/stryker4s/configuration/ for more information."
        )
      }
    }

    if (staticMutants.nonEmpty) {
      log.info(
        s"${staticMutants.size} mutant(s) detected as static. They will be skipped and marked as ${Color.Magenta("Ignored")}"
      )
      log.debug(s"Static mutant ids are: ${staticMutants.map(_._2.id).mkString(", ")}")
    }

    def mapPureMutants[K, V, VV](l: Seq[(K, V)], f: V => VV) =
      Stream.emits(l).map { case (k, v) => k -> f(v) }

    // Map all static mutants
    val static = mapPureMutants(staticMutants, staticMutant)
    // Map all no-coverage mutants
    val noCoverage = mapPureMutants(noCoverageMutants, noCoverageMutant(_))

    // Run all testable mutants
    val totalTestableMutants = testableMutants.size
    val testedMutants = Stream
      .emits(testableMutants)
      .through(testRunnerPool.run { case (testRunner, (path, mutant)) =>
        val coverageForMutant = coverageExclusions.coveredMutants.getOrElse(mutant.id, Seq.empty)
        IO(
          log.debug {
            val metadata = mutant.mutatedCode.metadata
            s"Running mutant ${mutant.id.value} (${metadata.mutatorName} at ${metadata.location.show})"
          }
        ) *>
          testRunner.runMutant(mutant, coverageForMutant).timed.flatMap { case (duration, result) =>
            IO(log.debug(s"Mutant ${mutant.id} tested in ${duration.toHumanReadable}")).as(path -> result)
          }
      })
      .observe(in => in.as(MutantTestedEvent(totalTestableMutants)).through(reporter.mutantTested))

    // Back to per-file structure
    implicit val pathOrdering: Ordering[Path] = implicitly[Ordering[nio.file.Path]].on[Path](_.toNioPath)
    implicit val mutantResultOrdering: Ordering[MutantResult] = Ordering.String.on[MutantResult](_.id)

    (static ++ noCoverage ++ testedMutants)
      .foldMap { case (a, b) => SortedMap(a -> Vector(b)) }
      .map(_.map { case (k, v) => k -> v.sorted })
      .compile
      .lastOrError
  }

  def initialTestRun(testRunner: TestRunner): IO[InitialTestRunResult] = {
    IO(log.info("Starting initial test run...")) *>
      testRunner.initialTestRun().flatMap { result =>
        if (!result.isSuccessful)
          IO.raiseError(
            InitialTestRunFailedException(
              "Initial test run failed. Please make sure your tests pass before running Stryker4s."
            )
          )
        else
          IO(log.info("Initial test run succeeded! Testing mutants...")).as(result)
      }
  }

  private def staticMutant(mutant: MutantWithId): MutantResult = mutant
    .toMutantResult(
      MutantStatus.Ignored,
      statusReason =
        "This is a 'static' mutant and can not be tested. If you still want to have this mutant tested, change your code to make this value initialize each time it is called.".some
    )
    .copy(static = true.some)

  private def noCoverageMutant(mutant: MutantWithId): MutantResult = mutant
    .toMutantResult(
      MutantStatus.NoCoverage,
      statusReason = "This mutant was not covered by any code.".some
    )

}
