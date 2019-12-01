package stryker4s.report

import grizzled.slf4j.Logging
import mutationtesting.{MutantResult, MutantStatus}
import stryker4s.config.Config
import stryker4s.model.{Mutant, MutantRunResult}
import stryker4s.run.threshold._

import scala.concurrent.duration.{Duration, MILLISECONDS}

class ConsoleReporter(implicit config: Config) extends FinishedRunReporter with ProgressReporter with Logging {
  private val startTime = System.currentTimeMillis()
  private[this] val mutationScoreString = "Mutation score:"

  override def reportMutationStart(mutant: Mutant): Unit = {
    info(s"Starting test-run ${mutant.id + 1}...")
  }

  override def reportMutationComplete(mutant: MutantRunResult, totalMutants: Int): Unit = {
    val id = mutant.mutant.id + 1
    info(s"Finished mutation run $id/$totalMutants (${((id / totalMutants.toDouble) * 100).round}%)")
  }

  override def reportRunFinished(runReport: FinishedRunReport): Unit = {
    val FinishedRunReport(report, metrics) = runReport
    val duration = Duration(System.currentTimeMillis() - startTime, MILLISECONDS)
    val (detectedMutants, rest) = report.files.toSeq flatMap {
      case (loc, f) => f.mutants.map(m => (loc, m))
    } partition (
        m => isDetected(m._2)
    )
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

  private def resultToString(name: String, mutants: Seq[(String, MutantResult)]): String =
    s"$name mutants:\n" +
      mutants
        .sortBy(m => m._2.id)
        .map(mutantDiff)
        .mkString("\n")

  private def mutantDiff(mrr: (String, MutantResult)): String = {
    val (filePath, mutant) = mrr
    val line = mutant.location.start.line + 1
    val col = mutant.location.start.column + 1

    s"""${mutant.id}. [${mutant.status}] [${mutant.mutatorName}]
       |$filePath:$line:$col
       |${mutant.replacement.linesIterator.map("\t" + _).mkString("\n")}
       |""".stripMargin
  }

  private def isDetected(mutant: MutantResult): Boolean =
    mutant.status == MutantStatus.Killed || mutant.status == MutantStatus.Timeout

  private def isUndetected(mutant: MutantResult): Boolean =
    mutant.status == MutantStatus.Survived || mutant.status == MutantStatus.NoCoverage

  implicit class DoubleRoundTwoDecimals(score: Double) {
    def roundDecimals(decimals: Int): Double =
      BigDecimal(score).setScale(decimals, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
}
