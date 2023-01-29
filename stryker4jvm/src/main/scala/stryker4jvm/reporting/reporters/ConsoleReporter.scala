package stryker4jvm.reporting.reporters

import cats.effect.IO
import fansi.{Color, EscapeAttr, Str}
import fs2.Pipe
import mutationtesting.{MutantResult, MutantStatus, Position}
import stryker4jvm.config.Config
import stryker4jvm.extensions.DurationExtensions.HumanReadableExtension
import stryker4jvm.extensions.NumberExtensions.RoundDecimalsExtension
import stryker4jvm.logging.FansiLogger
import stryker4jvm.reporting.{IOReporter, *}
import stryker4jvm.run.threshold.*

import scala.concurrent.duration.*

class ConsoleReporter()(implicit config: Config, log: FansiLogger) extends IOReporter {

  override def mutantTested: Pipe[IO, MutantTestedEvent, Nothing] = in => {
    val stream = in.zipWithIndex.map { case (l, r) => (l, r + 1) }
    // Log the first status right away, and then the latest every 0.5 seconds
    // 0.5 seconds is a good middle-ground between not printing too much and still feeling snappy
    (stream.head ++ stream.tail.debounce(0.5.seconds)).evalMap { case (event, progress) =>
      val total = event.totalMutants
      IO(log.info(s"Tested mutant $progress/$total (${((progress / total.toDouble) * 100).round}%)"))
    }.drain
  }

  override def onRunFinished(finishedRunEvent: FinishedRunEvent): IO[Unit] = IO {
    val report = finishedRunEvent.report
    val metrics = finishedRunEvent.metrics
    val duration = finishedRunEvent.duration

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

    val mutationScoreRounded = metrics.mutationScore.roundDecimals(2) match {
      case score if score.isNaN() => "n/a"
      case score                  => score.toString
    }

    def scoreString(color: EscapeAttr) = s"Mutation score: ${color(mutationScoreRounded)}%"

    val scoreStatus = ThresholdChecker.determineScoreStatus(metrics.mutationScore)
    scoreStatus match {
      case _ if metrics.mutationScore.isNaN() =>
        log.info(scoreString(Color.LightGray))
      case SuccessStatus => log.info(scoreString(Color.Green))
      case WarningStatus => log.warn(scoreString(Color.Yellow))
      case DangerStatus =>
        log.error(s"Mutation score dangerously low!")
        log.error(scoreString(Color.Red))
      case ErrorStatus =>
        log.error(
          s"Mutation score below threshold! ${scoreString(Color.Red)}. Threshold: ${config.thresholds.break}%"
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
    val startLine = lines(Math.max(0, startLinePos - 1)).substring(Math.max(0, startColumnPos - 1))
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
