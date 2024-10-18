import stryker4s.model.MutantId
import stryker4s.sbt.testrunner.TestInterfaceMapper
import stryker4s.testrunner.api.{CoverageTestNameMap, TestDefinition, TestFile, TestFileId}

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
    private val mutantCoverage = TrieMap.empty[MutantId, ConcurrentLinkedQueue[TestFileId]]

    private val tests = TrieMap.empty[TestFileId, TestFile]

    private val activeTest = new AtomicReference[TestFileId](TestFileId(-1))

    /** If we are currently collecting coverage analysis. If not we can skip it for performance
      */
    private val collectCoverage = new AtomicBoolean()

    /** Add a mutant to the current coverage report
      */
    def coverMutant(ids: Int*): Boolean = {
      if (collectCoverage.get()) {
        val currentTest = activeTest.get
        if (currentTest.value != -1) {
          ids.foreach { id =>
            val currentCovered = mutantCoverage.getOrElseUpdate(MutantId(id), new ConcurrentLinkedQueue())
            if (!currentCovered.contains(currentTest)) {
              currentCovered.add(currentTest)
              ()
            }
          }
        }
      }
      true // Always return true, `coverMutant` is called in the guard condition of the default mutation switch
    }

    /** Add the test selector to the currently active test.
      *
      * This is either the single test inside a suite if the framework supports it, or the name of the entire suite
      */
    protected[stryker4s] def appendDefinitionToActiveTest(definition: TestDefinition) =
      if (collectCoverage.get()) {
        val currentTestId = activeTest.get
        synchronized {
          val test = tests(currentTestId)
          tests.update(currentTestId, test.addDefinitions(definition))
        }
      }

    private val testIds = new AtomicInteger(0)

    /** Set the currently running test.
      *
      * This is to map the covered mutants with the test that was running at that time
      */
    protected[stryker4s] def setActiveTest(testName: String) =
      if (collectCoverage.get()) {
        val testId = TestFileId(testIds.getAndIncrement())
        val test = TestFile(testName, Seq.empty)
        activeTest.set(testId)
        tests.update(testId, test)
      }

    /** Collect coverage analysis during the provided function and return it in a tuple
      */
    protected[stryker4s] def collectCoverage[A](f: => A): (A, CoverageTestNameMap) = try {
      collectCoverage.set(true)

      val result = f

      (result, report())
    } finally {
      collectCoverage.set(false)
      mutantCoverage.clear()
      tests.clear()
      activeTest.set(TestFileId(-1))
      testIds.set(0)
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
      TestInterfaceMapper.toCoverageMap(mutantCoverage.map { case (k, v) => k -> v.asScala.toSeq }, tests.toMap)
    }
  }

  // Starting value of -1 means none
  private val activeMutationRef: AtomicInteger = new AtomicInteger(-1)

  def activeMutation: Int = activeMutationRef.get()

  protected[stryker4s] def activeMutation_=(mutation: Int): Unit = activeMutationRef.set(mutation)

}
