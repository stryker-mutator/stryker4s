package stryker4s.run

import cats.data.{EitherT, NonEmptyVector}
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import fs2.{text, Pipe, Stream}
import mutationtesting.{Metrics, MetricsResult}
import stryker4s.config.Config
import stryker4s.extension.FileExtensions.*
import stryker4s.extension.StreamExtensions.*
import stryker4s.extension.exception.{InitialTestRunFailedException, UnableToFixCompilerErrorsException}
import stryker4s.files.FilesFileResolver
import stryker4s.log.Logger
import stryker4s.model.*
import stryker4s.report.mapper.MutantRunResultMapper
import stryker4s.report.{FinishedRunEvent, MutantTestedEvent, Reporter}
import stryker4s.model.MutantResultsPerFile
import java.nio
import scala.concurrent.duration.*
import cats.data.NonEmptyList
import mutationtesting.MutantResult
import mutationtesting.MutantStatus
import scala.collection.immutable.SortedMap

class MutantRunner(
    createTestRunnerPool: Path => Either[NonEmptyList[CompilerErrMsg], Resource[IO, TestRunnerPool]],
    fileResolver: FilesFileResolver,
    reporter: Reporter
)(implicit config: Config, log: Logger) {
  val rollbackHandler: RollbackHandler = ???

  def apply(mutatedFiles: Seq[MutatedFile]): IO[RunResult] = {

    val withRollback = handleRollback(mutatedFiles)

    withRollback

  }

  def handleRollback(mutatedFiles: Seq[MutatedFile]) =
    EitherT(run(mutatedFiles))
      .leftFlatMap { errors =>
        log.info(s"Attempting to remove ${errors.size} mutants that gave a compile error...")
        // Retry once with the non-compiling mutants removed
        EitherT(
          rollbackHandler
            .rollbackFiles(errors, mutatedFiles)
            // TODO: cleanup
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
      tmpDir <- Files[IO].tempDirectory(Some(targetDir), "stryker4s-", None)
      _ <- Resource.eval(setupFiles(tmpDir, mutatedFiles.toSeq))
    } yield tmpDir
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
      Stream(mutatedFile.mutatedSource)
        .covary[IO]
        .through(text.utf8.encode)
        .through(Files[IO].writeAll(targetPath))
    }.parJoin(config.concurrency)

  private def runMutants(
      mutatedFiles: Seq[MutatedFile],
      testRunnerPool: TestRunnerPool,
      coverageExclusions: CoverageExclusions
  ): IO[MutantResultsPerFile] = {
    val allMutants = mutatedFiles.flatMap(m => m.mutants.toVector.map(m.fileOrigin.relativePath -> _))

    val (staticMutants, rest) = allMutants.partition(m => coverageExclusions.staticMutants.contains(m._2.id.globalId))

    val (noCoverageMutants, testableMutants) =
      rest.partition(m =>
        coverageExclusions.hasCoverage && !coverageExclusions.coveredMutants.contains(m._2.id.globalId)
      )

    // val compilerErrorMutants =
    //   mutatedFiles.flatMap(m => m.nonCompilingMutants.toList.map(m.fileOrigin.relativePath -> _))

    if (noCoverageMutants.nonEmpty) {
      log.info(
        s"${noCoverageMutants.size} mutants detected as having no code coverage. They will be skipped and marked as NoCoverage"
      )
      log.debug(s"NoCoverage mutant ids are: ${noCoverageMutants.map(_._2).mkString(", ")}")
    }

    if (staticMutants.nonEmpty) {
      log.info(
        s"${staticMutants.size} mutants detected as static. They will be skipped and marked as Ignored"
      )
      log.debug(s"Static mutant ids are: ${staticMutants.map(_._2).mkString(", ")}")
    }

    // TODO: move logging of compile-errors
    // if (compilerErrorMutants.nonEmpty) {
    //   log.info(
    //     s"${compilerErrorMutants.size} mutants gave a compiler error. They will be marked as such in the report."
    //   )
    //   log.debug(s"Non-compiling mutant ids are: ${compilerErrorMutants.map(_._2.id.globalId).mkString(", ")}")
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
        val coverageForMutant = coverageExclusions.coveredMutants.getOrElse(mutant.id.globalId, Seq.empty)
        IO(log.debug(s"Running mutant $mutant")) *>
          testRunner.runMutant(mutant, coverageForMutant).tupleLeft(path)
      })
      .observe(in => in.map(_ => MutantTestedEvent(totalTestableMutants)).through(reporter.mutantTested))

    // Back to per-file structure
    implicit val pathOrdering: Ordering[Path] = implicitly[Ordering[nio.file.Path]].on[Path](_.toNioPath)
    (static ++ noCoverage ++ testedMutants)
      .fold(SortedMap.empty[Path, Vector[MutantResult]]) { case (resultsMap, (path, result)) =>
        val results = resultsMap.getOrElse(path, Vector.empty) :+ result
        resultsMap + (path -> results)
      }
      .compile
      .lastOrError
      .map(_.map { case (k, v) => (k -> v.sortBy(_.id)) }.toMap)
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
              case _: NoCoverageInitialTestRun => CoverageExclusions(false, Map.empty, List.empty)
              case InitialTestRunCoverageReport(_, firstRun, secondRun, _) =>
                val firstRunMap = firstRun.report
                val secondRunMap = secondRun.report
                val staticMutants = (firstRunMap -- (secondRunMap.keys)).keys.toSeq

                val coveredMutants = firstRunMap.filterNot { case (id, _) => staticMutants.contains(id) }

                CoverageExclusions(true, staticMutants = staticMutants, coveredMutants = coveredMutants)
            }
          }
      }
  }

  private def staticMutant(mutant: MutantWithId): MutantResult = mutant
    .toMutantResult(MutantStatus.Ignored)
    .copy(
      description = Some(
        "This is a 'static' mutant and can not be tested. If you still want to have this mutant tested, change your code to make this value initialize each time it is called."
      ),
      static = Some(true)
    )

  private def noCoverageMutant(mutant: MutantWithId): MutantResult = mutant
    .toMutantResult(MutantStatus.Ignored)
    .copy(
      description = Some(
        "This is a 'static' mutant and can not be tested. If you still want to have this mutant tested, change your code to make this value initialize each time it is called."
      ),
      static = Some(true)
    )

  private def compileErrorMutant(mutant: MutantWithId): MutantResult = mutant.toMutantResult(MutantStatus.CompileError)

  case class CoverageExclusions(
      hasCoverage: Boolean,
      coveredMutants: Map[Int, Seq[String]],
      staticMutants: Seq[Int]
  )

}
