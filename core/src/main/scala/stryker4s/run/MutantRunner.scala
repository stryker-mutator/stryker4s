package stryker4s.run

import java.nio.file.Path
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

import better.files.File
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
import stryker4s.report.{FinishedRunReport, Reporter}

abstract class MutantRunner(sourceCollector: SourceCollector, reporter: Reporter)(implicit
    config: Config,
    log: Logger,
    timer: Timer[IO],
    cs: ContextShift[IO]
) extends MutantRunResultMapper {
  type Context <: TestRunnerContext

  def apply(mutatedFiles: List[MutatedFile]): IO[MetricsResult] =
    prepareEnv(mutatedFiles)
      .use(context =>
        initialTestRun(context).flatMap { coverageExclusions =>
          runMutants(mutatedFiles, context, coverageExclusions).timed
        }
      )
      .flatMap(t => createAndReportResults(t._1, t._2))

  def createAndReportResults(runResults: Map[Path, List[MutantRunResult]], duration: FiniteDuration) = for {
    time <- Clock[IO].realTime(TimeUnit.MILLISECONDS)
    report = toReport(runResults)
    metrics = Metrics.calculateMetrics(report)
    reportsLocation = config.baseDir / "target/stryker4s-report" / time.toString()
    _ <- reporter.reportRunFinished(FinishedRunReport(report, metrics, duration, reportsLocation))
  } yield metrics

  def prepareEnv(mutatedFiles: Seq[MutatedFile]): Resource[IO, Context] = for {
    blocker <- Blocker[IO]
    targetDir = (config.baseDir / "target").path
    _ <- Resource.liftF(io.file.createDirectories[IO](blocker, targetDir))
    tmpDir <- Resource.apply[IO, Path](
      io.file.tempDirectoryResource[IO](blocker, targetDir, "stryker4s-").allocated.map(_._1 -> IO.unit)
    )
    _ <- Resource.liftF(setupFiles(blocker, tmpDir, mutatedFiles.toSeq))
    context <- initializeTestContext(tmpDir)
  } yield context

  private def setupFiles(blocker: Blocker, tmpDir: Path, mutatedFiles: Seq[MutatedFile]): IO[Unit] =
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

  def writeOriginalFile(blocker: Blocker, tmpDir: Path): Pipe[IO, Path, Unit] =
    in =>
      in.evalMapChunk { file =>
        val newSubPath = file.inSubDir(tmpDir)

        IO(log.debug(s"Copying $file to $newSubPath")) *>
          io.file.createDirectories[IO](blocker, newSubPath.getParent()) *>
          io.file.copy[IO](blocker, file, newSubPath).void
      }

  def writeMutatedFile(blocker: Blocker, tmpDir: Path): Pipe[IO, MutatedFile, Unit] =
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
      context: Context,
      coverageExclusions: CoverageExclusions
  ): IO[Map[Path, List[MutantRunResult]]] = {
    val totalMutants = mutatedFiles.flatMap(_.mutants).size

    mutatedFiles
      .map(m => m.fileOrigin.relativePath -> m.mutants.toList)
      .traverse { case (subPath, mutants) =>
        mutants
          .traverse { mutant =>
            if (coverageExclusions.staticMutants.contains(mutant.id)) {
              IO.pure(ignored(mutant))
            } else if (coverageExclusions.hasCoverage && !coverageExclusions.coveredMutants.contains(mutant.id)) {
              IO.pure(NoCoverage(mutant))
            } else
              reportAndRunMutant(mutant, context, totalMutants)
          }
          .tupleLeft(subPath)
      }
      .map(_.toMap)
  }

  private def reportAndRunMutant(mutant: Mutant, context: Context, totalMutants: Int): IO[MutantRunResult] =
    for {
      _ <- reporter.reportMutationStart(mutant)
      result <- runMutant(mutant, context)
      _ <- reporter.reportMutationComplete(result, totalMutants)
    } yield result

  def runMutant(mutant: Mutant, context: Context): IO[MutantRunResult]

  def initialTestRun(context: Context): IO[CoverageExclusions] = {
    IO(log.info("Starting initial test run...")) *>
      runInitialTest(context).flatMap { result =>
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

  private def ignored(mutant: Mutant) = Ignored(
    mutant,
    Some(
      "This is a 'static' mutant and can not be tested. If you still want to have this mutant tested, change your code to make this value initialize each time it is called."
    )
  )

  def runInitialTest(context: Context): IO[InitialTestRunResult]

  def initializeTestContext(tmpDir: File): Resource[IO, Context]

  case class CoverageExclusions(hasCoverage: Boolean, coveredMutants: Seq[Int], staticMutants: Seq[Int])

}
