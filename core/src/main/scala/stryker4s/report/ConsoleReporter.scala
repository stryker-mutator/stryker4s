package stryker4s.report

import grizzled.slf4j.Logging
import mutationtesting.{MutantResult, MutantStatus}
import stryker4s.config.Config
import stryker4s.model.{Mutant, MutantRunResult}
import stryker4s.run.threshold._
import scala.concurrent.duration.{Duration, MILLISECONDS}
import mutationtesting.Position
import cats.effect.IO

class ConsoleReporter(implicit config: Config) extends FinishedRunReporter with ProgressReporter with Logging {
  private val startTime = System.currentTimeMillis()
  private[this] val mutationScoreString = "Mutation score:"

  override def reportMutationStart(mutant: Mutant): IO[Unit] =
    IO {
      info(s"Starting test-run ${mutant.id + 1}...")
    }

  override def reportMutationComplete(mutant: MutantRunResult, totalMutants: Int): IO[Unit] =
    IO {
      val id = mutant.mutant.id + 1
      info(s"Finished mutation run $id/$totalMutants (${((id / totalMutants.toDouble) * 100).round}%)")
    }

  override def reportRunFinished(runReport: FinishedRunReport): IO[Unit] =
    IO {
      val FinishedRunReport(report, metrics) = runReport
      val duration = Duration(System.currentTimeMillis() - startTime, MILLISECONDS)
      val (detectedMutants, rest) = report.files.toSeq flatMap {
        case (loc, f) => f.mutants.map(m => (loc, m, f.source))
      } partition (m => isDetected(m._2))
      val (undetectedMutants, _) = rest partition (m => isUndetected(m._2))
      info(s"Mutation run finished! Took ${duration.toSeconds} seconds")
      info(
        s"Total mutants: ${metrics.totalMutants}, detected: ${metrics.totalDetected}, undetected: ${metrics.totalUndetected}"
      )

      debug(resultToString("Detected", detectedMutants))
      info(resultToString("Undetected", undetectedMutants))

      val scoreStatus = ThresholdChecker.determineScoreStatus(metrics.mutationScore)
      val mutationScoreRounded = metrics.mutationScore.roundDecimals(2)
      scoreStatus match {
        case SuccessStatus => info(s"$mutationScoreString $mutationScoreRounded%")
        case WarningStatus => warn(s"$mutationScoreString $mutationScoreRounded%")
        case DangerStatus =>
          error(s"Mutation score dangerously low!")
          error(s"$mutationScoreString $mutationScoreRounded%")
        case ErrorStatus =>
          error(
            s"Mutation score below threshold! Score: $mutationScoreRounded%. Threshold: ${config.thresholds.break}%"
          )
      }
    }

  private def resultToString(name: String, mutants: Seq[(String, MutantResult, String)]): String =
    s"$name mutants:\n" +
      mutants
        .sortBy(m => m._2.id)
        .map({ case (filePath, mutant, testResult) => mutantDiff(filePath, mutant, testResult) })
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
