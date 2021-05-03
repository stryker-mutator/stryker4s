package stryker4s.run

import cats.effect.{IO, Resource}
import cats.syntax.functor._
import fs2.io.file.Files
import fs2.{text, Pipe, Stream}
import mutationtesting.{Metrics, MetricsResult}
import stryker4s.config.Config
import stryker4s.extension.FileExtensions._
import stryker4s.extension.exception.InitialTestRunFailedException
import stryker4s.log.Logger
import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.mapper.MutantRunResultMapper
import stryker4s.report.{FinishedRunEvent, MutantTestedEvent, Reporter}

import java.nio.file.Path
import scala.concurrent.duration._

class MutantRunner(
    createTestRunnerPool: Path => Resource[IO, TestRunnerPool],
    sourceCollector: SourceCollector,
    reporter: Reporter
)(implicit config: Config, log: Logger)
    extends MutantRunResultMapper {

  def apply(mutatedFiles: List[MutatedFile]): IO[MetricsResult] =
    prepareEnv(mutatedFiles)
      .flatMap(createTestRunnerPool)
      .use { testRunnerPool =>
        testRunnerPool.loan
          .use(initialTestRun)
          .flatMap { coverageExclusions =>
            runMutants(mutatedFiles, testRunnerPool, coverageExclusions).timed
          }
      }
      .flatMap(t => createAndReportResults(t._1, t._2))

  def createAndReportResults(duration: FiniteDuration, runResults: Map[Path, List[MutantRunResult]]) = for {
    time <- IO.realTime
    report = toReport(runResults)
    metrics = Metrics.calculateMetrics(report)
    reportsLocation = config.baseDir / "target/stryker4s-report" / time.toMillis.toString()
    _ <- reporter.onRunFinished(FinishedRunEvent(report, metrics, duration, reportsLocation))
  } yield metrics

  def prepareEnv(mutatedFiles: Seq[MutatedFile]): Resource[IO, Path] = {
    val targetDir = (config.baseDir / "target").path
    for {
      _ <- Resource.eval(Files[IO].createDirectories(targetDir))
      tmpDir <- Files[IO].tempDirectory(dir = Some(targetDir), prefix = "stryker4s-")
      _ <- Resource.eval(setupFiles(tmpDir, mutatedFiles.toSeq))
    } yield tmpDir
  }

  private def setupFiles(tmpDir: Path, mutatedFiles: Seq[MutatedFile]): IO[Unit] =
    IO(log.info("Setting up mutated environment...")) *>
      IO(log.debug("Using temp directory: " + tmpDir)) *> {
        val mutatedPaths = mutatedFiles.map(_.fileOrigin)
        val unmutatedFilesStream =
          Stream
            .evalSeq(IO(sourceCollector.filesToCopy.filterNot(mutatedPaths.contains).toSeq))
            .map(_.path)
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
          Files[IO].createDirectories(newSubPath.getParent()) *>
          Files[IO].copy(file, newSubPath).void
      }

  def writeMutatedFile(tmpDir: Path): Pipe[IO, MutatedFile, Unit] =
    _.parEvalMap(config.concurrency) { mutatedFile =>
      val targetPath = mutatedFile.fileOrigin.path.inSubDir(tmpDir)
      IO(log.debug(s"Writing ${mutatedFile.fileOrigin} file to $targetPath")) *>
        Files[IO]
          .createDirectories(targetPath.getParent())
          .as((mutatedFile, targetPath))
    }.map { case (mutatedFile, targetPath) =>
      Stream(mutatedFile.tree.syntax)
        .covary[IO]
        .through(text.utf8Encode)
        .through(Files[IO].writeAll(targetPath))
    }.parJoin(config.concurrency)

  private def runMutants(
      mutatedFiles: List[MutatedFile],
      testRunnerPool: TestRunnerPool,
      coverageExclusions: CoverageExclusions
  ): IO[Map[Path, List[MutantRunResult]]] = {

    val allMutants = mutatedFiles.flatMap(m => m.mutants.toList.map(m.fileOrigin.relativePath -> _))

    val (staticMutants, rest) = allMutants.partition(m => coverageExclusions.staticMutants.contains(m._2.id))
    val (noCoverageMutants, testableMutants) =
      rest.partition(m => coverageExclusions.hasCoverage && !coverageExclusions.coveredMutants.contains(m._2.id))

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

    def mapPureMutants[K, V, VV](l: List[(K, V)], f: V => VV) =
      Stream.emits(l).map { case (k, v) => k -> f(v) }

    // Map all static mutants
    val static = mapPureMutants(staticMutants, staticMutant(_))
    // Map all no-coverage mutants
    val noCoverage = mapPureMutants(noCoverageMutants, (m: Mutant) => NoCoverage(m))

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
    (static ++ noCoverage ++ testedMutants)
      .fold(Map.empty[Path, List[MutantRunResult]]) { case (resultsMap, (path, result)) =>
        val results = resultsMap.getOrElse(path, List.empty) :+ result
        resultsMap + (path -> results)
      }
      .compile
      .lastOrError
  }

  def initialTestRun(testRunner: TestRunner): IO[CoverageExclusions] = {
    IO(log.info("Starting initial test run...")) *>
      testRunner.initialTestRun().flatMap { result =>
        if (!result.fold(identity, _.isSuccessful))
          IO.raiseError(
            InitialTestRunFailedException(
              "Initial test run failed. Please make sure your tests pass before running Stryker4s."
            )
          )
        else
          IO(log.info("Initial test run succeeded! Testing mutants...")).as {
            result match {
              case Left(_) => CoverageExclusions(false, List.empty, List.empty)
              case Right(InitialTestRunCoverageReport(_, firstRun, secondRun)) =>
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
