package stryker4s.report

import cats.effect.IO
import fansi.{Color, EscapeAttr, Str}
import fs2.Pipe
import mutationtesting.{MutantResult, MutantStatus, Position}
import stryker4s.config.Config
import stryker4s.extension.DurationExtensions.*
import stryker4s.extension.NumberExtensions.*
import stryker4s.log.Logger
import stryker4s.run.threshold.*

import scala.concurrent.duration.*

class ConsoleReporter()(implicit config: Config, log: Logger) extends Reporter {

  override def mutantTested: Pipe[IO, MutantTestedEvent, Nothing] = in => {
    val stream = in.zipWithIndex.map { case (l, r) => (l, r + 1) }
    // Log the first status right away, and then the latest every 0.5 seconds
    // 0.5 seconds is a good middle-ground between not printing too much and still feeling snappy
    (stream.head ++ stream.tail.debounce(0.5.seconds)).evalMap { case (event, progress) =>
      val total = event.totalMutants
      IO(log.info(s"Tested mutant $progress/$total (${((progress / total.toDouble) * 100).round}%)"))
    }.drain
  }

  override def onRunFinished(runReport: FinishedRunEvent): IO[Unit] =
    IO {
      val FinishedRunEvent(report, metrics, duration, _) = runReport

      log.info(s"Mutation run finished! Took ${duration.toHumanReadable}")
      log.info(s"Total mutants: ${Color.Cyan(metrics.totalMutants.toString())}, detected: ${Color
          .Green(metrics.totalDetected.toString())}, undetected: ${Color.Red(metrics.totalUndetected.toString())}")

      val (detectedMutants, rest) = report.files.toSeq
        .flatMap { case (loc, f) =>
          f.mutants.map(m => (loc, m, f.source))
        }
        .partition(m => isDetected(m._2))
      val (undetectedMutants, _) = rest partition (m => isUndetected(m._2))

      if (detectedMutants.nonEmpty) log.debug(resultToString("Detected", detectedMutants))
      if (undetectedMutants.nonEmpty) log.info(resultToString("Undetected", undetectedMutants))

      def roundScore(score: Double): String = score.roundDecimals(2) match {
        case score if score.isNaN() => "n/a"
        case score                  => score.toString
      }

      val mutationScoreRounded = roundScore(metrics.mutationScore)
      val mutationScoreCoveredCode = roundScore(metrics.mutationScoreBasedOnCoveredCode)

      val scoreStatus = ThresholdChecker.determineScoreStatus(metrics.mutationScore)
      val coveredStatus = ThresholdChecker.determineScoreStatus(metrics.mutationScoreBasedOnCoveredCode)

      def colorForStatus(status: ScoreStatus): EscapeAttr = status match {
        case _ if metrics.mutationScore.isNaN() => Color.LightGray
        case SuccessStatus                      => Color.Green
        case WarningStatus                      => Color.Yellow
        case DangerStatus                       => Color.Red
        case ErrorStatus                        => Color.Red
      }

      def scoreString = {
        val color = colorForStatus(scoreStatus)
        val coveredColor =
          colorForStatus(coveredStatus)

        if (mutationScoreRounded != mutationScoreCoveredCode)
          s"Mutation score: ${color(mutationScoreRounded)}% (of total), ${coveredColor(mutationScoreCoveredCode)}% (of covered code)"
        else s"Mutation score: ${color(mutationScoreRounded)}%"
      }

      scoreStatus match {
        case _ if metrics.mutationScore.isNaN() => log.info(scoreString)
        case SuccessStatus                      => log.info(scoreString)
        case WarningStatus                      => log.warn(scoreString)
        case DangerStatus =>
          log.error(s"Mutation score dangerously low!")
          log.error(scoreString)
        case ErrorStatus =>
          log.error(
            s"Mutation score below threshold! $scoreString. Threshold: ${config.thresholds.break}%"
          )
      }
    }

  private def resultToString(name: String, mutants: Seq[(String, MutantResult, String)]): Str = {
    val mutantsStr =
      if (mutants.nonEmpty)
        Str.join(
          mutants
            .sortBy(m => m._2.id)
            .map { case (filePath, mutant, testResult) => mutantDiff(filePath, mutant, testResult) },
          "\n"
        )
      else Str("")

    Str(s"$name mutants:\n", mutantsStr)
  }

  private def mutantDiff(filePath: String, mutant: MutantResult, source: String): Str = {
    val line = mutant.location.start.line
    val col = mutant.location.start.column

    Str.join(
      Seq(
        s"${mutant.id}. [${Color.Magenta(mutant.status.toString())}] [${Color.LightGray(mutant.mutatorName)}]",
        Str.join(Seq(Color.Blue(filePath), Color.Yellow(line.toString()), Color.Yellow(col.toString())), ":"),
        Color.Red(s"-${tabbed(findOriginal(source, mutant))}"),
        Color.Green(s"+${tabbed(mutant.replacement)}"),
        ""
      ),
      "\n"
    )
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

}
