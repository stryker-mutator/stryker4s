import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}

import scala.collection.concurrent.TrieMap

import sbt.testing.Fingerprint
import stryker4s.api.testprocess.CoverageReport
import stryker4s.sbt.testrunner.TestInterfaceMapper

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
    def coverMutant(id: Int) = {
      if (collectCoverage.get()) {
        val currentTest = activeTest.get
        if (currentTest != null) {
          val currentCovered = coveredTests.getOrElseUpdate(id, new ConcurrentLinkedQueue())
          currentCovered.add(currentTest)
        }
      }
    }

    /** Set the currently running test. This is needed to map the covered mutants with the test that was running at that time
      */
    def setActiveTest(fingerPrint: Fingerprint) = if (collectCoverage.get()) activeTest.set(fingerPrint)

    /** Collect coverage analysis during the provided function and return it in a tuple
      */
    def collectCoverage[A](f: => A): (A, CoverageReport) = try {
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
      ScalaVersionCompat.mapAsJava(coveredTests.map { case (mutant, tests) =>
        mutant -> ScalaVersionCompat.queueAsScala(tests).map(TestInterfaceMapper.toFingerprint(_)).toArray
      })
  }

  // Starting value of  -1 means none
  private val activeMutationRef: AtomicInteger = new AtomicInteger(-1)

  def activeMutation: Int = activeMutationRef.get()

  def activeMutation_=(mutation: Int): Unit = activeMutationRef.set(mutation)

}
