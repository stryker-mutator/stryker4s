package stryker4s.sbt.testrunner

import sbt.testing.{Event, EventHandler, Framework, Status, Task}
import stryker4s.api.testprocess._
import stryker4s.logTimed

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator
import scala.util.control.NonFatal
import java.util.concurrent.atomic.AtomicInteger

sealed trait TestRunner {
  def runMutation(mutation: Int, fingerprints: Seq[Fingerprint]): Status
  def initialTestRun(): Status
}

class SbtTestInterfaceRunner(context: TestProcessContext) extends TestRunner with TestInterfaceMapper {

  val testFunctions: Option[(Int, Seq[Fingerprint])] => Status = {
    val tasks = logTimed("TestRunnerSetupTasks") {
      val cl = getClass().getClassLoader()
      context.testGroups.flatMap(testGroup => {
        val RunnerOptions(args, remoteArgs) = testGroup.runnerOptions.get
        val framework = cl.loadClass(testGroup.frameworkClass).getConstructor().newInstance().asInstanceOf[Framework]
        val runner = framework.runner(args.toArray, remoteArgs.toArray, cl)
        runner.tasks(testGroup.taskDefs.map(toSbtTaskDef).toArray)
      })
    }
    (mutation: Option[(Int, Seq[Fingerprint])]) => {
      val tasksToRun = mutation match {
        case Some((_, fingerprints)) =>
          tasks.filter(t => fingerprints.map(toSbtFingerprint).contains(t.taskDef().fingerprint()))
        case None => tasks
      }
      mutation.foreach { case (mutantId, _) => stryker4s.activeMutation = mutantId }
      val testsRan = new AtomicInteger()

      try runTests(tasksToRun, new AtomicReference(Status.Success), testsRan)

      finally mutation.foreach { case (mutantId, _) => println(s"Ran ${testsRan.get()} for mutant $mutantId") }
    }
  }

  def runMutation(mutation: Int, fingerprints: Seq[Fingerprint]) = {
    logTimed("TestRunnerRunMutation")(testFunctions(Some((mutation, fingerprints))))
  }

  def initialTestRun(): Status = {
    testFunctions(None)
  }

  private def runTests(
      testTasks: Seq[Task],
      status: AtomicReference[Status],
      testsRan: AtomicInteger
  ): sbt.testing.Status = {
    val eventHandler = new StatusEventHandler(status, testsRan)

    val newTasks = testTasks.flatMap(task =>
      status.get() match {
        // Fail early
        case Status.Failure => Array.empty[Task]
        case Status.Error   => Array.empty[Task]
        case _ =>
          stryker4s.coverage.setActiveTest(task.taskDef().fingerprint())
          task.execute(eventHandler, Array.empty)
      }
    )
    if (newTasks.nonEmpty) logTimed("TestRunnerNewTasks")(runTests(newTasks, status, testsRan))
    else status.get()
  }

  class StatusEventHandler(status: AtomicReference[Status], testsRan: AtomicInteger) extends EventHandler {
    override def handle(event: Event) = logTimed("TestRunnerTestEventHandler") {
      testsRan.incrementAndGet()
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
