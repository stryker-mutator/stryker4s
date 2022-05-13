import stryker4s.api.testprocess.CoverageTestNameMap
import stryker4s.sbt.testrunner.TestInterfaceMapper

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}
import java.util.concurrent.{ConcurrentLinkedQueue, TimeUnit}
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.FiniteDuration

package object stryker4s {

  /** object to collect coverage analysis on the mutated code
    */
  object coverage {

    /** We have no idea how tests will run their code, so the coverage analysis needs to be able to handle concurrency
      */
    private val coveredTests = TrieMap.empty[Int, ConcurrentLinkedQueue[String]]

    private val activeTest = new AtomicReference[String]

    /** If we are currently collecting coverage analysis. If not we can skip it for performance
      */
    private val collectCoverage = new AtomicBoolean

    /** Add a mutant to the current coverage report
      */
    def coverMutant(ids: Int*): Boolean = {
      if (collectCoverage.get()) {
        val currentTest = activeTest.get
        if (currentTest != null) {
          ids.foreach { id =>
            val currentCovered = coveredTests.getOrElseUpdate(id, new ConcurrentLinkedQueue)
            if (!currentCovered.contains(currentTest))
              currentCovered.add(currentTest)
          }
        }
      }
      true // Always return true, `coverMutant` is called in the guard condition of the default mutation switch
    }

    /** Set the currently running test.
      *
      * This is to map the covered mutants with the test that was running at that time
      */
    protected[stryker4s] def setActiveTest(testName: String) =
      if (collectCoverage.get()) activeTest.set(testName)

    /** Collect coverage analysis during the provided function and return it in a tuple
      */
    protected[stryker4s] def collectCoverage[A](f: => A): (A, CoverageTestNameMap) = try {
      collectCoverage.set(true)

      val result = f

      (result, report())
    } finally {
      collectCoverage.set(false)
      coveredTests.clear()
    }

    /** Time a given function and return the result and the duration of that function as a tuple
      */
    protected[stryker4s] def timed[A](f: => A): (FiniteDuration, A) = {
      val start = System.nanoTime()
      val result = f
      val duration = FiniteDuration(System.nanoTime() - start, TimeUnit.NANOSECONDS)
      (duration, result)
    }

    /** Build the coverage report from the collected data
      */
    private def report(): CoverageTestNameMap = {
      import scala.jdk.CollectionConverters.*
      TestInterfaceMapper.toCoverageMap(coveredTests.map { case (k, v) => k -> v.asScala.toSeq })
    }
  }

  // Starting value of -1 means none
  private val activeMutationRef: AtomicInteger = new AtomicInteger(-1)

  def activeMutation: Int = activeMutationRef.get()

  protected[stryker4s] def activeMutation_=(mutation: Int): Unit = activeMutationRef.set(mutation)

}
