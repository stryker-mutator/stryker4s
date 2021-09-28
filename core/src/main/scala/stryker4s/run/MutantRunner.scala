package stryker4s.run

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.syntax.either._
import cats.syntax.functor._
import fs2.io.file.{Files, Path}
import fs2.{text, Pipe, Stream}
import mutationtesting.{Metrics, MetricsResult}
import stryker4s.config.Config
import stryker4s.extension.FileExtensions._
import stryker4s.extension.StreamExtensions._
import stryker4s.extension.exception.{InitialTestRunFailedException, UnableToFixCompilerErrorsException}
import stryker4s.files.FilesFileResolver
import stryker4s.log.Logger
import stryker4s.model.{CompilerErrMsg, _}
import stryker4s.report.mapper.MutantRunResultMapper
import stryker4s.report.{FinishedRunEvent, MutantTestedEvent, Reporter}

import java.nio
import scala.collection.immutable.SortedMap
import scala.concurrent.duration._

class MutantRunner(
    createTestRunnerPool: Path => Either[NonEmptyList[CompilerErrMsg], Resource[IO, TestRunnerPool]],
    fileResolver: FilesFileResolver,
    reporter: Reporter
)(implicit config: Config, log: Logger)
    extends MutantRunResultMapper {

  def apply(mutateFiles: Seq[CompilerErrMsg] => IO[Seq[MutatedFile]]): IO[MetricsResult] = {
    mutateFiles(Seq.empty).flatMap { mutatedFiles =>
      run(mutatedFiles)
        .flatMap {
          case Right(metrics) => IO.pure(metrics.asRight)
          case Left(errors)   =>
            //Retry once with the non-compiling mutants removed
            mutateFiles(errors.toList).flatMap(run)
        }
        .flatMap {
          case Right(metrics) => IO.pure(metrics)
          //Failed at remove the non-compiling mutants
          case Left(errs) => IO.raiseError(UnableToFixCompilerErrorsException(errs.toList))
        }
    }
  }

  def run(mutatedFiles: Seq[MutatedFile]): IO[Either[NonEmptyList[CompilerErrMsg], MetricsResult]] = {
    prepareEnv(mutatedFiles).use { path =>
      createTestRunnerPool(path) match {
        case Left(errs) => IO.pure(errs.asLeft)
        case Right(testRunnerPoolResource) =>
          testRunnerPoolResource.use { testRunnerPool =>
            testRunnerPool.loan
              .use(initialTestRun)
              .flatMap { coverageExclusions =>
                runMutants(mutatedFiles, testRunnerPool, coverageExclusions).timed
              }
              .flatMap(t => createAndReportResults(t._1, t._2))
              .map(Right(_))
          }
      }
    }
  }

  def createAndReportResults(duration: FiniteDuration, runResults: Map[Path, Seq[MutantRunResult]]) = for {
    time <- IO.realTime
    report = toReport(runResults)
    metrics = Metrics.calculateMetrics(report)
    reportsLocation = config.baseDir / "target/stryker4s-report" / time.toMillis.toString()
    _ <- reporter.onRunFinished(FinishedRunEvent(report, metrics, duration, reportsLocation))
  } yield metrics

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
  ): IO[Map[Path, Seq[MutantRunResult]]] = {

    val allMutants = mutatedFiles.flatMap(m => m.mutants.toList.map(m.fileOrigin.relativePath -> _))

    val (staticMutants, rest) = allMutants.partition(m => coverageExclusions.staticMutants.contains(m._2.id.globalId))

    val (noCoverageMutants, testableMutants) =
      rest.partition(m =>
        coverageExclusions.hasCoverage && !coverageExclusions.coveredMutants.contains(m._2.id.globalId)
      )

    val compilerErrorMutants =
      mutatedFiles.flatMap(m => m.nonCompilingMutants.toList.map(m.fileOrigin.relativePath -> _))

    if (noCoverageMutants.nonEmpty) {
      log.info(
        s"${noCoverageMutants.size} mutants detected as having no code coverage. They will be skipped and marked as NoCoverage"
      )
      log.debug(s"NoCoverage mutant ids are: ${noCoverageMutants.map(_._2.id.globalId).mkString(", ")}")
    }

    if (staticMutants.nonEmpty) {
      log.info(
        s"${staticMutants.size} mutants detected as static. They will be skipped and marked as Ignored"
      )
      log.debug(s"Static mutant ids are: ${staticMutants.map(_._2.id.globalId).mkString(", ")}")
    }

    if (compilerErrorMutants.nonEmpty) {
      log.info(
        s"${compilerErrorMutants.size} mutants gave a compiler error. They will be marked as such in the report."
      )
      log.debug(s"Non-compiling mutant ids are: ${compilerErrorMutants.map(_._2.id.globalId).mkString(", ")}")
    }

    def mapPureMutants[K, V, VV](l: Seq[(K, V)], f: V => VV) =
      Stream.emits(l).map { case (k, v) => k -> f(v) }

    // Map all static mutants
    val static = mapPureMutants(staticMutants, staticMutant(_))
    // Map all no-coverage mutants
    val noCoverage = mapPureMutants(noCoverageMutants, (m: Mutant) => NoCoverage(m))
    // Map all no-compiling mutants
    val noCompiling = mapPureMutants(compilerErrorMutants, (m: Mutant) => CompileError(m))

    // Run all testable mutants
    val totalTestableMutants = testableMutants.size
    val testedMutants = Stream
      .emits(testableMutants)
      .through(testRunnerPool.run { case (testRunner, (path, mutant)) =>
        IO(log.debug(s"Running mutant $mutant")) *>
          testRunner.runMutant(mutant).tupleLeft(path)
      })
      .observe(in => in.map(_ => MutantTestedEvent(totalTestableMutants)).through(reporter.mutantTested))

    // Back to per-file structure
    implicit val pathOrdering: Ordering[Path] = implicitly[Ordering[nio.file.Path]].on[Path](_.toNioPath)
    (static ++ noCoverage ++ noCompiling ++ testedMutants)
      .fold(SortedMap.empty[Path, Seq[MutantRunResult]]) { case (resultsMap, (path, result)) =>
        val results = resultsMap.getOrElse(path, Seq.empty) :+ result
        resultsMap + (path -> results)
      }
      .compile
      .lastOrError
      .map(_.map { case (k, v) => (k -> v.sortBy(_.mutant.id.globalId)) })
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
              case _: NoCoverageInitialTestRun => CoverageExclusions(false, List.empty, List.empty)
              case InitialTestRunCoverageReport(_, firstRun, secondRun, _) =>
                val firstRunMap = firstRun.toMap
                val secondRunMap = secondRun.toMap
                val staticMutants = (firstRunMap -- (secondRunMap.keys)).keys.toSeq

                val coveredMutants = firstRunMap
                  .filterNot { case (id, _) => staticMutants.contains(id) }
                  .keys
                  .toSeq

                CoverageExclusions(true, staticMutants = staticMutants, coveredMutants = coveredMutants)
            }
          }
      }
  }

  private def staticMutant(mutant: Mutant): MutantRunResult = Ignored(
    mutant,
    Some(
      "This is a 'static' mutant and can not be tested. If you still want to have this mutant tested, change your code to make this value initialize each time it is called."
    )
  )

  case class CoverageExclusions(hasCoverage: Boolean, coveredMutants: Seq[Int], staticMutants: Seq[Int])

}
