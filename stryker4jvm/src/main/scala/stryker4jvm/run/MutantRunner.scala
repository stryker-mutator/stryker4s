package stryker4jvm.run

import cats.data.{EitherT, NonEmptyList}
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import fs2.{text, Pipe, Stream}
import mutationtesting.{MutantResult, MutantStatus}
import stryker4jvm.config.Config
import stryker4jvm.core.logging.Logger
import stryker4jvm.core.model.{AST, MutantWithId}
import stryker4jvm.exception.{InitialTestRunFailedException, UnableToFixCompilerErrorsException}
import stryker4jvm.extensions.FileExtensions.PathExtensions
import stryker4jvm.extensions.MutantExtensions.ToMutantResultExtension
import stryker4jvm.files.FilesFileResolver
import stryker4jvm.model.*
import stryker4jvm.reporting.{IOReporter, MutantTestedEvent}

import java.nio
import scala.collection.immutable.SortedMap
import scala.collection.mutable

class MutantRunner(
    createTestRunnerPool: Path => Either[NonEmptyList[CompilerErrMsg], Resource[IO, TestRunnerPool]],
    fileResolver: FilesFileResolver,
    rollbackHandler: RollbackHandler,
    reporter: IOReporter[Config]
)(implicit config: Config, log: Logger) {

  def apply(mutatedFiles: Seq[MutatedFile]): IO[RunResult] = {

    val withRollback = handleRollback(mutatedFiles)

    withRollback

  }

  def handleRollback(mutatedFiles: Seq[MutatedFile]): IO[RunResult] =
    EitherT(run(mutatedFiles))
      .leftFlatMap { errors =>
        log.info(s"Attempting to remove ${errors.size} mutants that gave a compile error...")
        // Retry once with the non-compiling mutants removed
        EitherT(
          rollbackHandler
            .rollbackFiles()
            // TODO: handle rollbacks in a different place
            .flatTraverse { case RollbackResult(newFiles, rollbackedMutants) =>
              run(newFiles).map { result =>
                result.map { r =>
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
          testRunnerPool.loan
            .use(testrunner => initialTestRun(testrunner))
            .flatMap(coverageExclusions => runMutants(mutatedFiles, testRunnerPool, coverageExclusions).timed)
            .map(t => RunResult(t._2, t._1))
        }
      }
    }
  }

  def prepareEnv(mutatedFiles: Seq[MutatedFile]): Resource[IO, Path] = {
    val targetDir = config.baseDir / "target"
    for {
      _ <- Resource.eval(Files[IO].createDirectories(targetDir))
      tmpDir <- prepareTmpDir(targetDir)
      _ <- Resource.eval(setupFiles(tmpDir, mutatedFiles.toSeq))
    } yield tmpDir
  }

   private def prepareTmpDir(targetDir: Path): Resource[IO, Path] = {
    val tmpDirCreated = if (config.staticTmpDir) {
      val staticTmpDir = targetDir / "stryker4s-tmpDir"
      Files[IO].createDirectory(staticTmpDir).as(staticTmpDir)
    } else {
      Files[IO].createTempDirectory(Some(targetDir), "stryker4s-", None)
    }
    Resource.makeCase(tmpDirCreated)(tmpDirFinalizeCase)
  }

  private def tmpDirFinalizeCase: (Path, Resource.ExitCase) => IO[Unit] = {
    case (tmpDir, Resource.ExitCase.Succeeded | Resource.ExitCase.Canceled) =>
      if (config.cleanTmpDir) {
        Files[IO].deleteRecursively(tmpDir)
      } else {
        IO(
          log.info(
            s"Not deleting $tmpDir (turn off cleanTmpDir to disable this). Please clean it up manually."
          )
        )
      }
    case (tmpDir, _: Resource.ExitCase.Errored) =>
      // Enable the user do some manual actions before she retries.
      IO(
        log.warn(
          s"Not deleting $tmpDir after error. Please clean it up manually."
        )
      )
  }

  private def setupFiles(tmpDir: Path, mutatedFiles: Seq[MutatedFile]): IO[Unit] =
    IO(log.info("Setting up mutated environment...")) *>
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
      in.parEvalMapUnordered(config.concurrency) { file =>
        val newSubPath = file.inSubDir(tmpDir)

        IO(log.debug(s"Copying $file to $newSubPath")) *>
          Files[IO].createDirectories(newSubPath.parent.get) *>
          Files[IO].copy(file, newSubPath).void
      }

  def writeMutatedFile(tmpDir: Path): Pipe[IO, MutatedFile, Unit] =
    _.parEvalMap(config.concurrency) { mutatedFile =>
      val targetPath = mutatedFile.fileOrigin.inSubDir(tmpDir)
      IO(log.debug(s"Writing ${mutatedFile.fileOrigin} file to $targetPath")) *>
        Files[IO]
          .createDirectories(targetPath.parent.get)
          .as((mutatedFile, targetPath))
    }.map { case (mutatedFile, targetPath) =>
      Stream(mutatedFile.mutatedSource.syntax)
        .covary[IO]
        .through(text.utf8.encode)
        .through(Files[IO].writeAll(targetPath))
    }.parJoin(config.concurrency)

  private def runMutants(
      mutatedFiles: Seq[MutatedFile],
      testRunnerPool: TestRunnerPool,
      coverageExclusions: CoverageExclusions
  ): IO[MutantResultsPerFile] = {
    val allMutants = mutatedFiles.flatMap(m => m.mutants.toVector.map(m.fileOrigin -> _))

    val (staticMutants, rest) = allMutants.partition(m => coverageExclusions.staticMutants.contains(m._2.id))

    val (noCoverageMutants, testableMutants) =
      rest.partition(m => coverageExclusions.hasCoverage && !coverageExclusions.coveredMutants.contains(m._2.id))

    // val compilerErrorMutants =
    //   mutatedFiles.flatMap(m => m.nonCompilingMutants.toList.map(m.fileOrigin.relativePath -> _))

    if (noCoverageMutants.nonEmpty) {
      log.info(
        s"${noCoverageMutants.size} mutants detected as having no code coverage. They will be skipped and marked as NoCoverage"
      )
      log.debug(s"NoCoverage mutant ids are: ${noCoverageMutants.map(_._2.id).mkString(", ")}")
    }

    if (staticMutants.nonEmpty) {
      log.info(
        s"${staticMutants.size} mutants detected as static. They will be skipped and marked as Ignored"
      )
      log.debug(s"Static mutant ids are: ${staticMutants.map(_._2.id).mkString(", ")}")
    }

    // TODO: move logging of compile-errors
    // if (compilerErrorMutants.nonEmpty) {
    //   log.info(
    //     s"${compilerErrorMutants.size} mutants gave a compiler error. They will be marked as such in the report."
    //   )
    //   log.debug(s"Non-compiling mutant ids are: ${compilerErrorMutants.map(_._2.id.value).mkString(", ")}")
    // }

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
        IO(log.debug(s"Running mutant $mutant")) *>
          testRunner.runMutant(mutant, coverageForMutant).tupleLeft(path)
      })
      .observe(in => in.as(MutantTestedEvent(totalTestableMutants)).through(reporter.mutantTested))

    // Back to per-file structure
    implicit val pathOrdering: Ordering[Path] = implicitly[Ordering[nio.file.Path]].on[Path](_.toNioPath)
    implicit val mutantResultOrdering: Ordering[MutantResult] = Ordering.String.on[MutantResult](_.id)
    type MutantResultBuilder = mutable.Builder[MutantResult, Vector[MutantResult]]

    (static ++ noCoverage ++ testedMutants)
      .fold(SortedMap.empty[Path, MutantResultBuilder]) { case (resultsMap, (path, result)) =>
        val results: MutantResultBuilder = resultsMap.getOrElse(path, Vector.newBuilder) += result
        resultsMap + (path -> results)
      }
      .compile
      .lastOrError
      .map(_.map { case (k, v) => k -> v.result().sorted })
  }

  def initialTestRun(testRunner: TestRunner): IO[CoverageExclusions] = {
    IO(log.info("Starting initial test run...")) *>
      testRunner.initialTestRun().flatMap { result =>
        if (!result.isSuccessful)
          IO.raiseError(
            InitialTestRunFailedException(
              "Initial test run failed. Please make sure your tests pass before running Stryker4s."
            )
          )
        else
          IO(log.info("Initial test run succeeded! Testing mutants...")).as {
            result match {
              case _: NoCoverageInitialTestRun => CoverageExclusions(hasCoverage = false, Map.empty, List.empty)
              case InitialTestRunCoverageReport(_, firstRun, secondRun, _) =>
                val firstRunMap = firstRun.report
                val secondRunMap = secondRun.report
                val staticMutants = (firstRunMap -- (secondRunMap.keys)).keys.toSeq

                val coveredMutants = firstRunMap.filterNot { case (id, _) => staticMutants.contains(id) }

                CoverageExclusions(hasCoverage = true, staticMutants = staticMutants, coveredMutants = coveredMutants)
            }
          }
      }
  }

  private def staticMutant(mutant: MutantWithId[AST]): MutantResult = mutant
    .toMutantResult(MutantStatus.Ignored)
    .copy(
      description = Some(
        "This is a 'static' mutant and can not be tested. If you still want to have this mutant tested, change your code to make this value initialize each time it is called."
      ),
      static = true.some
    )

  private def noCoverageMutant(mutant: MutantWithId[AST]): MutantResult = mutant
    .toMutantResult(MutantStatus.Ignored)
    .copy(
      description =
        "This is a 'static' mutant and can not be tested. If you still want to have this mutant tested, change your code to make this value initialize each time it is called.".some,
      static = true.some
    )

  case class CoverageExclusions(
      hasCoverage: Boolean,
      coveredMutants: Map[Int, Seq[String]],
      staticMutants: Seq[Int]
  )

}
