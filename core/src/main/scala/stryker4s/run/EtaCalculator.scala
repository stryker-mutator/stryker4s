package stryker4s.run

import grizzled.slf4j.Logging

import scala.concurrent.duration._
import language.postfixOps

class EtaCalculator(amountOfMutants: Int) extends Logging {
  val runResults: Array[Long] = new Array(amountOfMutants)
  private[this] val stream = Iterator.range(0, amountOfMutants)

  def time[T](fun: => T, log: String => Unit): T = {
    val startTime = System.currentTimeMillis()

    // Run the function
    val result: T = fun

    // Measure duration and store in array
    val duration = System.currentTimeMillis() - startTime
    val runNr = stream.next()
    runResults(runNr) = duration

    // Log estimate
    log(calculateETA(runNr + 1))

    result
  }

  def calculateETA(currentRun: Int): String = {
    val from = math.max(0, currentRun - 5) // Base estimate on last 5 runs
    val average = runResults.slice(from, currentRun).sum / (currentRun - from)
    prettyPrintDuration((amountOfMutants - currentRun) * average milliseconds)
  }

  private def prettyPrintDuration(duration: Duration): String = {
    val durationInSeconds = duration.toSeconds
    val hours = durationInSeconds / 3600
    val minutes = (durationInSeconds % 3600) / 60
    val seconds = durationInSeconds % 60

    (if(hours > 0) s"$hours hours " else "" ) + s"$minutes minutes and $seconds seconds"
  }
}
