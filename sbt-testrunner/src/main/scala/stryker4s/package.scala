import sbt.testing.Fingerprint
import stryker4s.api.testprocess.{CoverageReport, Fingerprints}
import stryker4s.sbt.testrunner.TestInterfaceMapper

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}
import scala.collection.concurrent.TrieMap

package object stryker4s {

  /** object to collect coverage analysis on the mutated code
    */
  object coverage {

    /** We have no idea how tests will run their code, so the coverage analysis needs to be able to handle concurrency
      */
    private val coveredTests = TrieMap.empty[Int, ConcurrentLinkedQueue[Fingerprint]]

    private val activeTest = new AtomicReference[Fingerprint]()

    /** If we are currently collecting coverage analysis. If not we can skip it for performance
      */
    private val collectCoverage = new AtomicBoolean()

    /** Add a mutant to the current coverage report
      */
    def coverMutant(ids: Int*): Boolean = {
      if (collectCoverage.get()) {
        val currentTest = activeTest.get
        if (currentTest != null) {
          ids.foreach { id =>
            val currentCovered = coveredTests.getOrElseUpdate(id, new ConcurrentLinkedQueue())
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
    protected[stryker4s] def setActiveTest(fingerPrint: Fingerprint) =
      if (collectCoverage.get()) activeTest.set(fingerPrint)

    /** Collect coverage analysis during the provided function and return it in a tuple
      */
    protected[stryker4s] def collectCoverage[A](f: => A): (A, CoverageReport) = try {
      collectCoverage.set(true)

      val result = f

      (result, report())
    } finally {
      collectCoverage.set(false)
      coveredTests.clear()
    }

    /** Build the coverage report from the collected data
      */
    private def report(): CoverageReport =
      coveredTests.toMap.map { case (mutant, tests) =>
        mutant -> Fingerprints(ScalaVersionCompat.queueAsScala(tests).map(TestInterfaceMapper.toFingerprint(_)).toSeq)
      }
  }

  // Starting value of  -1 means none
  private val activeMutationRef: AtomicInteger = new AtomicInteger(-1)

  def activeMutation: Int = activeMutationRef.get()

  protected[stryker4s] def activeMutation_=(mutation: Int): Unit = activeMutationRef.set(mutation)

}
