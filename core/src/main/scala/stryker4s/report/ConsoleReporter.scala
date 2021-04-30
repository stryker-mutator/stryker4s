package stryker4s.report

import cats.effect.{Deferred, IO}
import cats.syntax.all._
import fs2.{INothing, Pipe}
import mutationtesting.{MutantResult, MutantStatus, Position}
import stryker4s.config.Config
import stryker4s.extension.DurationExtensions._
import stryker4s.log.Logger
import stryker4s.model.{Mutant, MutantRunResult}
import stryker4s.run.threshold._

import java.nio.file.Path
import scala.concurrent.duration._

class ConsoleReporter(total: Deferred[IO, Int])(implicit config: Config, log: Logger) extends Reporter {
  private[this] val mutationScoreString = "Mutation score:"

  override def mutantPlaced: Pipe[IO, Mutant, INothing] =
    _.fold(0)({ case (size, _) => size + 1 }).evalMap(total.complete(_)).drain

  override def mutantTested: Pipe[IO, (Path, MutantRunResult), INothing] = in => {
    val stream = in.void.zipWithIndex
    // Log the first status right away, and then the latest every 2 seconds
    (stream.head ++ stream.tail.debounce(1.second))
      .map(_._2)
      .evalMap { progress =>
        total.tryGet.flatMap(_.traverse_ { total =>
          IO(log.info(s"Starting mutation run ${progress}/${total} (${((progress / total.toDouble) * 100).round}%)"))
        })
      }
      .drain
  }

  override def onRunFinished(runReport: FinishedRunEvent): IO[Unit] =
    IO {
      val FinishedRunEvent(report, metrics, duration, _) = runReport

      log.info(s"Mutation run finished! Took ${duration.toHumanReadable}")
      log.info(
        s"Total mutants: ${metrics.totalMutants}, detected: ${metrics.totalDetected}, undetected: ${metrics.totalUndetected}"
      )

      val (detectedMutants, rest) = report.files.toSeq
        .flatMap { case (loc, f) =>
          f.mutants.map(m => (loc, m, f.source))
        }
        .partition(m => isDetected(m._2))
      val (undetectedMutants, _) = rest partition (m => isUndetected(m._2))

      log.debug(resultToString("Detected", detectedMutants))
      log.info(resultToString("Undetected", undetectedMutants))

      val scoreStatus = ThresholdChecker.determineScoreStatus(metrics.mutationScore)
      val mutationScoreRounded = metrics.mutationScore.roundDecimals(2)
      scoreStatus match {
        case SuccessStatus => log.info(s"$mutationScoreString $mutationScoreRounded%")
        case WarningStatus => log.warn(s"$mutationScoreString $mutationScoreRounded%")
        case DangerStatus =>
          log.error(s"Mutation score dangerously low!")
          log.error(s"$mutationScoreString $mutationScoreRounded%")
        case ErrorStatus =>
          log.error(
            s"Mutation score below threshold! Score: $mutationScoreRounded%. Threshold: ${config.thresholds.break}%"
          )
      }
    }

  private def resultToString(name: String, mutants: Seq[(String, MutantResult, String)]): String =
    s"$name mutants:\n" +
      mutants
        .sortBy(m => m._2.id)
        .map { case (filePath, mutant, testResult) => mutantDiff(filePath, mutant, testResult) }
        .mkString("\n")

  private def mutantDiff(filePath: String, mutant: MutantResult, source: String): String = {
    val line = mutant.location.start.line
    val col = mutant.location.start.column

    s"""${mutant.id}. [${mutant.status}] [${mutant.mutatorName}]
       |$filePath:$line:$col
       |-${tabbed(findOriginal(source, mutant))}
       |+${tabbed(mutant.replacement)}
       |""".stripMargin
  }

  private def tabbed(string: String) = string.linesIterator.map("\t" + _).mkString("\n")

  private def findOriginal(source: String, mutant: MutantResult): String = {
    val Position(startLinePos, startColumnPos) = mutant.location.start
    val Position(endLinePos, endColumnPos) = mutant.location.end
    val lines = source.linesIterator.toSeq
    val startLine = lines(startLinePos - 1).substring(startColumnPos - 1)
    endLinePos - startLinePos match {
      // Mutation is 1 line
      case 0 => startLine.substring(0, endColumnPos - startColumnPos)
      // Mutation is two lines
      case 1 =>
        val endLine = lines(endLinePos - 1).substring(0, endColumnPos - 1)
        s"$startLine\n$endLine"
      // Mutation is multiple lines
      case _ =>
        val linesBetween = lines.slice(startLinePos, endLinePos - 1)
        val endLine = lines(endLinePos - 1).substring(0, endColumnPos - 1)
        (startLine +: linesBetween :+ endLine).mkString("\n")
    }
  }

  private def isDetected(mutant: MutantResult): Boolean =
    mutant.status == MutantStatus.Killed || mutant.status == MutantStatus.Timeout

  private def isUndetected(mutant: MutantResult): Boolean =
    mutant.status == MutantStatus.Survived || mutant.status == MutantStatus.NoCoverage

  implicit class DoubleRoundTwoDecimals(score: Double) {
    def roundDecimals(decimals: Int): Double =
      if (!score.isNaN())
        BigDecimal(score).setScale(decimals, BigDecimal.RoundingMode.HALF_UP).toDouble
      else
        score
  }
}
