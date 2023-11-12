package stryker4s.sbt.testrunner

import sbt.testing.{Event, EventHandler, Framework, Status, Task}
import stryker4s.testrunner.api.testprocess.*

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import java.util.function.UnaryOperator
import scala.annotation.tailrec
import scala.util.control.NonFatal

sealed trait TestRunner {
  def runMutation(mutation: Int, fingerprints: Seq[String]): TestRunResult
  def initialTestRun(): TestRunResult
}

private[stryker4s] case class TestRunResult(status: Status, testsCompleted: Int)

class SbtTestInterfaceRunner(context: TestProcessContext) extends TestRunner with TestInterfaceMapper {

  val testFunctions: Option[(Int, Seq[String])] => TestRunResult = {
    val tasks = {
      val cl = getClass().getClassLoader()
      context.testGroups.flatMap { testGroup =>
        val RunnerOptions(args, remoteArgs) = testGroup.runnerOptions.get
        val framework = cl.loadClass(testGroup.frameworkClass).getConstructor().newInstance().asInstanceOf[Framework]
        val runner = framework.runner(args.toArray, remoteArgs.toArray, cl)
        runner.tasks(testGroup.taskDefs.map(toSbtTaskDef).toArray)
      }
    }
    (mutation: Option[(Int, Seq[String])]) => {
      val tasksToRun = mutation match {
        case Some((_, testNames)) =>
          tasks.filter(t => testNames.contains(t.taskDef().fullyQualifiedName()))
        case None => tasks
      }
      val testsCompleted = new AtomicInteger(0)
      val statusRef = new AtomicReference[Status](Status.Success)
      val eventHandler = new StatusEventHandler(statusRef, testsCompleted)
      mutation.foreach { case (mutantId, _) => stryker4s.activeMutation = mutantId }
      val status = runTests(tasksToRun, statusRef, eventHandler)

      TestRunResult(status, testsCompleted.get())
    }
  }

  override def runMutation(mutation: Int, testNames: Seq[String]): TestRunResult = {
    testFunctions(Some((mutation, testNames)))
  }

  override def initialTestRun(): TestRunResult = {
    testFunctions(None)
  }

  @tailrec
  private def runTests(
      testTasks: Seq[Task],
      status: AtomicReference[Status],
      eventHandler: EventHandler
  ): sbt.testing.Status = {

    val newTasks = testTasks.flatMap(task =>
      status.get() match {
        // Fail early
        case Status.Failure => Array.empty[Task]
        case Status.Error   => Array.empty[Task]
        case _ =>
          stryker4s.coverage.setActiveTest(task.taskDef().fullyQualifiedName())
          task.execute(eventHandler, Array.empty)
      }
    )
    if (newTasks.nonEmpty) runTests(newTasks, status, eventHandler)
    else status.get()
  }

  class StatusEventHandler(status: AtomicReference[Status], testsCompleted: AtomicInteger) extends EventHandler {
    override def handle(event: Event) = {
      testsCompleted.incrementAndGet()
      if (event.status() != Status.Success) {
        println(s"Test unsuccessful: ${event.fullyQualifiedName()} status ${event.status()} with ${event.throwable()}")

        // Re-throw Fatal exceptions to restart the testrunner
        val maybethrowable =
          if (event.throwable().isDefined())
            Some(event.throwable().get()).filterNot(NonFatal(_))
          else None
        maybethrowable.foreach { t =>
          println(s"Fatal exception reported by testrunner. Re-throwing $t")
          throw t
        }
      }
      status.updateAndGet(new UnaryOperator[Status]() {
        override def apply(old: Status) = {
          combineStatus(old, event.status())
        }
      })
      ()
    }
  }
}
