package stryker4s.run

import java.nio.file.Path
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

import cats.effect._
import cats.syntax.all._
import fs2.{io, text, Pipe, Stream}
import mutationtesting.{Metrics, MetricsResult}
import stryker4s.config.Config
import stryker4s.extension.CatsEffectExtensions._
import stryker4s.extension.FileExtensions._
import stryker4s.extension.exception.InitialTestRunFailedException
import stryker4s.log.Logger
import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.mapper.MutantRunResultMapper
import stryker4s.report.{FinishedRunEvent, Progress, Reporter, StartMutationEvent}
import cats.effect.{ Resource, Temporal }

class MutantRunner(
    testRunnerResource: Path => Resource[IO, TestRunner],
    sourceCollector: SourceCollector,
    reporter: Reporter
)(implicit config: Config, log: Logger, timer: Temporal[IO], cs: ContextShift[IO])
    extends MutantRunResultMapper {

  def apply(mutatedFiles: List[MutatedFile]): IO[MetricsResult] =
    prepareEnv(mutatedFiles)
      .use(testRunner =>
        initialTestRun(testRunner).flatMap { coverageExclusions =>
          runMutants(mutatedFiles, testRunner, coverageExclusions).timed
        }
      )
      .flatMap(t => createAndReportResults(t._1, t._2))

  def createAndReportResults(runResults: Map[Path, List[MutantRunResult]], duration: FiniteDuration) = for {
    time <- Clock[IO].realTime(TimeUnit.MILLISECONDS)
    report = toReport(runResults)
    metrics = Metrics.calculateMetrics(report)
    reportsLocation = config.baseDir / "target/stryker4s-report" / time.toString()
    _ <- reporter.onRunFinished(FinishedRunEvent(report, metrics, duration, reportsLocation))
  } yield metrics

  def prepareEnv(mutatedFiles: Seq[MutatedFile]): Resource[IO, TestRunner] = for {
    blocker <- Resource.unit[IO]
    targetDir = (config.baseDir / "target").path
    _ <- Resource.eval(io.file.createDirectories[IO](blocker, targetDir))
    tmpDir <- io.file.tempDirectoryResource[IO](blocker, targetDir, "stryker4s-")
    _ <- Resource.eval(setupFiles(blocker, tmpDir, mutatedFiles.toSeq))
    testRunner <- testRunnerResource(tmpDir)
  } yield testRunner

  private def setupFiles(tmpDir: Path, mutatedFiles: Seq[MutatedFile]): IO[Unit] =
    IO(log.info("Setting up mutated environment...")) *>
      IO(log.debug("Using temp directory: " + tmpDir)) *> {
        val mutatedPaths = mutatedFiles.map(_.fileOrigin)
        val unmutatedFilesStream =
          Stream
            .evalSeq(IO(sourceCollector.filesToCopy.toSeq))
            .filter(!mutatedPaths.contains(_))
            .map(_.path)
            .through(writeOriginalFile(blocker, tmpDir))

        val mutatedFilesStream = Stream
          .emits(mutatedFiles)
          .through(writeMutatedFile(blocker, tmpDir))

        (unmutatedFilesStream merge
          mutatedFilesStream).compile.drain
      }

  def writeOriginalFile(tmpDir: Path): Pipe[IO, Path, Unit] =
    in =>
      in.evalMapChunk { file =>
        val newSubPath = file.inSubDir(tmpDir)

        IO(log.debug(s"Copying $file to $newSubPath")) *>
          io.file.createDirectories[IO](blocker, newSubPath.getParent()) *>
          io.file.copy[IO](blocker, file, newSubPath).void
      }

  def writeMutatedFile(tmpDir: Path): Pipe[IO, MutatedFile, Unit] =
    in =>
      in.evalMapChunk { mutatedFile =>
        val targetPath = mutatedFile.fileOrigin.path.inSubDir(tmpDir)
        IO(log.debug(s"Writing ${mutatedFile.fileOrigin} file to $targetPath")) *>
          io.file
            .createDirectories[IO](blocker, targetPath.getParent())
            .as((mutatedFile, targetPath))
      }.flatMap { case (mutatedFile, targetPath) =>
        Stream(mutatedFile.tree.syntax)
          .covary[IO]
          .through(text.utf8Encode)
          .through(io.file.writeAll(targetPath, blocker))
      }

  private def runMutants(
      mutatedFiles: List[MutatedFile],
      testRunner: TestRunner,
      coverageExclusions: CoverageExclusions
  ): IO[Map[Path, List[MutantRunResult]]] = {

    val allMutants = mutatedFiles.flatMap(m => m.mutants.toList.map(m.fileOrigin.relativePath -> _))

    val (staticMutants, rest) = allMutants.partition(m => coverageExclusions.staticMutants.contains(m._2.id))
    val (noCoverageMutants, testableMutants) =
      rest.partition(m => coverageExclusions.hasCoverage && !coverageExclusions.coveredMutants.contains(m._2.id))

    val totalTestableMutants = testableMutants.size

    if (noCoverageMutants.nonEmpty) {
      log.info(
        s"${noCoverageMutants.size} mutants detected as having no code coverage. They will be skipped and marked as NoCoverage"
      )
      log.debug(s"NoCoverage mutant ids are: ${noCoverageMutants.mkString(", ")}")
    }

    if (staticMutants.nonEmpty) {
      log.info(
        s"${staticMutants.size} mutants detected as static. They will be skipped and marked as Ignored"
      )
      log.debug(s"Static mutant ids are: ${staticMutants.mkString(", ")}")
    }

    def mapPureMutants[K, V, VV](l: List[(K, V)], f: V => VV) = l.map { case (k, v) => k -> f(v) }.pure[IO]

    // Map all static mutants
    val static = mapPureMutants(staticMutants, staticMutant(_))
    // Map all no-coverage mutants
    val noCoverage = mapPureMutants(noCoverageMutants, (m: Mutant) => NoCoverage(m))
    // Run all testable mutants
    val testedMutants = testableMutants.zipWithIndex.traverse { case ((subPath, mutant), progress) =>
      reporter.onMutationStart(StartMutationEvent(Progress(progress + 1, totalTestableMutants))) *>
        testRunner
          .runMutant(mutant)
          .tupleLeft(subPath)
    }

    // Back to per-file structure
    static
      .combine(noCoverage)
      .combine(testedMutants)
      .map(
        _.groupBy(_._1)
          .map { case (path, files) => path -> files.map(_._2) }
      )
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
                  .filterNot({ case (id, _) => staticMutants.contains(id) })
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
