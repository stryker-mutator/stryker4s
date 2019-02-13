package stryker4s.run

import grizzled.slf4j.Logging

import scala.concurrent.duration._
import language.postfixOps
import scala.collection.mutable

class EtaCalculator(amountOfMutants: Int) extends Logging {
  private[this] val maxQueueSize = 10
  val runResults: mutable.Queue[Long] = new mutable.Queue
  private[this] val stream = Iterator.range(amountOfMutants - 1, -1, -1)

  def time[T](fun: => T): (T, String) = {
    val startTime = System.currentTimeMillis()

    // Run the function
    val result: T = fun

    // Measure duration and store
    saveRunResult(System.currentTimeMillis() - startTime)

    (result, calculateETA(stream.next))
  }

  def saveRunResult(duration: Long): Unit = {
    if (runResults.size >= maxQueueSize) runResults.dequeue()
    runResults.enqueue(duration)
  }

  def calculateETA(runsRemaining: Int): String = {
    prettyPrintDuration(runsRemaining * calculateMedian milliseconds)
  }

  private def calculateMedian: Long = {
    val sorted = runResults.sorted
    if (runResults.size % 2 == 0) {
      val first = sorted.drop((runResults.size / 2) - 1).head
      val second = sorted.drop(runResults.size / 2).head
      (first + second) / 2
    } else {
      sorted.drop((runResults.size - 1) / 2).head
    }
  }

  private def prettyPrintDuration(duration: Duration): String = {
    val durationInSeconds = duration.toSeconds
    val hours = durationInSeconds / 3600
    val minutes = (durationInSeconds % 3600) / 60
    val seconds = durationInSeconds % 60

    (if (hours > 0) s"$hours hours " else "") + s"$minutes minutes and $seconds seconds"
  }
}
