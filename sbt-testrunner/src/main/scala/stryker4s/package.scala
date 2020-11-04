import scala.collection.mutable
import java.util.concurrent.atomic.AtomicInteger
import sbt.testing.Fingerprint
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Consumer

package object stryker4s {
  object coverage {

    /** We have no idea how tests will run their code, so the coverage analysis needs to be able to handle concurrency
      * The main action is to append, and at the end collect once which makes ConcurrentQueue a good candidate
      */
    private val coveredTests = new ConcurrentLinkedQueue[(Fingerprint, Int)]()

    private val activeTest = new AtomicReference[Fingerprint]()

    /** TODO: add per-test reporting
      *
      * @param id
      */
    def coverMutant(id: Int): Unit = {
      val currentTest = activeTest.get
      if (currentTest != null) {
        coveredTests.add((currentTest, id))
      }
      ()
    }

    def setActiveTest(fingerPrint: Fingerprint) = activeTest.set(fingerPrint)

    def report() = {
      val buffer = mutable.Buffer[(Fingerprint, Int)]()
      coveredTests.forEach(new Consumer[(Fingerprint, Int)] {
        override def accept(t: (Fingerprint, Int)): Unit = {
          buffer += t
          ()
        }
      })

      buffer.toSeq
        .groupBy(_._1)
        .map({ case (key, values) => key -> values.map(_._2) })
    }

    def clear(): Unit = coveredTests.clear()

  }

  object activeMutation {

    private val activeMutationRef = new AtomicInteger(-1) // '-1' means no mutation is active at the start

    def get: Int = activeMutationRef.get()

    def activate(mutation: Int): Unit = activeMutationRef.set(mutation)

  }
}
