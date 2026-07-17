package stryker4s.testrunner

import sbt.testing.{Event, EventHandler, Framework, Status, Task}
import stryker4s.model.MutantId
import stryker4s.testrunner.api.*

import java.io.{PrintWriter, StringWriter}
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.annotation.tailrec
import scala.util.control.NonFatal

sealed trait TestRunner {
  def runMutation(mutation: MutantId, fingerprints: Seq[String]): TestRunResult
  def initialTestRun(): TestRunResult
}

private[stryker4s] case class TestRunResult(status: Status, testsCompleted: Int, failedTests: Seq[FailedTestDefinition])

class SbtTestInterfaceRunner(context: TestProcessContext) extends TestRunner with TestInterfaceMapper {

  private val classLoader = getClass().getClassLoader()

  /** Create a new Test framework instance, create task definitions (filtered for coverage), and runs the tests for a
    * mutation (or the initial test-run when `mutation` is `None`).
    */
  private def runTestFunction(mutation: Option[(MutantId, Seq[String])]): TestRunResult = {
    val testsCompleted = new AtomicInteger(0)
    val statusRef = new AtomicReference[Status](Status.Success)
    val failedTestsRef = new AtomicReference[Seq[FailedTestDefinition]](Vector.empty)

    val eventHandler = mutation match {
      case Some(_) => new MutantRunEventHandler(statusRef, testsCompleted, failedTestsRef)
      case None    => new InitialTestRunEventHandler(statusRef)
    }

    mutation.foreach { case (mutantId, _) => stryker4s.activeMutation = mutantId.value }

    val coveredTestNames: Option[Set[String]] = mutation.map(_._2.toSet)

    context.testGroups.foreach { testGroup =>
      val taskDefs =
        testGroup.taskDefs.filter(td => coveredTestNames.forall(_.contains(td.fullyQualifiedName)))

      // Skip running if there are no tests to run, or if another test group has already failed
      if (taskDefs.nonEmpty && statusRef.get() != Status.Failure && statusRef.get() != Status.Error) {
        val RunnerOptions(args, remoteArgs) = testGroup.runnerOptions.get
        val framework =
          classLoader.loadClass(testGroup.frameworkClass).getConstructor().newInstance().asInstanceOf[Framework]
        val runner = framework.runner(args.toArray, remoteArgs.toArray, classLoader)
        try {
          val tasks = runner.tasks(taskDefs.map(toSbtTaskDef).toArray)
          val _ = runTests(tasks.toIndexedSeq, statusRef, eventHandler)
        } finally {
          // Signal the framework that the run is done so it can release any (background) resources it allocated.
          val doneMessage = runner.done()
          if (doneMessage.nonEmpty) println(doneMessage)
        }
      }
    }

    TestRunResult(statusRef.get(), testsCompleted.get(), failedTestsRef.get())
  }

  override def runMutation(mutation: MutantId, testNames: Seq[String]): TestRunResult = {
    runTestFunction(Some((mutation, testNames)))
  }

  override def initialTestRun(): TestRunResult = {
    runTestFunction(None)
  }

  private def runTests(
      testTasks: Seq[Task],
      status: AtomicReference[Status],
      eventHandler: EventHandler
  ): sbt.testing.Status = {

    @tailrec
    def runTasks(testTasks: Seq[Task]): sbt.testing.Status = {
      val newTasks = testTasks.flatMap(task =>
        status.get() match {
          // Fail early
          case Status.Failure => Array.empty[Task]
          case Status.Error   => Array.empty[Task]
          case _              =>
            stryker4s.coverage.setActiveTest(task.taskDef().fullyQualifiedName())
            task.execute(eventHandler, Array.empty)
        }
      )
      if (newTasks.nonEmpty) runTasks(newTasks)
      else status.get()
    }

    runTasks(testTasks)
  }

  class InitialTestRunEventHandler(status: AtomicReference[Status]) extends EventHandler {
    private val testDefinitionIds = new AtomicInteger(0)
    override def handle(event: Event) = {
      val td = TestDefinition.of(TestDefinitionId(testDefinitionIds.getAndIncrement()), testNameFromEvent(event))
      stryker4s.coverage.appendDefinitionToActiveTest(td)
      status.updateAndGet(old => combineStatus(old, event.status()))
      ()
    }

  }

  class MutantRunEventHandler(
      status: AtomicReference[Status],
      testsCompleted: AtomicInteger,
      failedTestIdsRef: AtomicReference[Seq[FailedTestDefinition]]
  ) extends EventHandler {
    override def handle(event: Event) = {
      val testName = testNameFromEvent(event)
      testsCompleted.incrementAndGet()
      if (event.status() != Status.Success) {
        println(s"Test unsuccessful: $testName status ${event.status()} with ${event.throwable()}")

        // Re-throw Fatal exceptions to restart the testrunner
        if (event.throwable().isDefined()) {
          val t = event.throwable().get()
          if (!NonFatal(t)) {
            println(s"Fatal exception reported by testrunner. Re-throwing $t")
            throw t
          }
        }
        failedTestIdsRef.updateAndGet(
          _ :+ FailedTestDefinition.of(
            fullyQualifiedName = event.fullyQualifiedName,
            name = testName,
            message = toOption(event.throwable()).map(throwableToString)
          )
        )
        ()
      }

      status.updateAndGet(old => combineStatus(old, event.status()))
      ()
    }
  }

  /** Gets the stack trace from a Throwable as a String, including suppressed and cause exceptions.
    *
    * @see
    *   https://commons.apache.org/proper/commons-lang/apidocs/src-html/org/apache/commons/lang3/exception/ExceptionUtils.html#line-463
    */
  private def throwableToString(throwable: Throwable): String = {
    val sw = new StringWriter()

    throwable.printStackTrace(new PrintWriter(sw, true))
    sw.toString()
  }
}
