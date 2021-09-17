package stryker4s.sbt.testrunner

import sbt.testing.{Event, EventHandler, Framework, Status, Task}
import stryker4s.api.testprocess._

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator
import scala.annotation.tailrec
import scala.util.control.NonFatal

sealed trait TestRunner {
  def runMutation(mutation: Int): Status
  def initialTestRun(): Status
}

class SbtTestInterfaceRunner(context: TestProcessContext) extends TestRunner with TestInterfaceMapper {

  val cl = getClass().getClassLoader()
  val tasks = context.testGroups
    .flatMap(testGroup => {
      val RunnerOptions(args, remoteArgs) = testGroup.runnerOptions
      val framework = cl.loadClass(testGroup.frameworkClass).getConstructor().newInstance().asInstanceOf[Framework]
      val runner = framework.runner(args.toArray, remoteArgs.toArray, cl)
      runner.tasks(testGroup.taskDefs.map(toSbtTaskDef).toArray)
    })
    .toArray
  val testFunctions: Option[Int] => Status = { (mutation: Option[Int]) =>
    {
      mutation.foreach(stryker4s.activeMutation = _)
      runTests(tasks, new AtomicReference(Status.Success))
    }
  }

  def runMutation(mutation: Int) = {
    testFunctions(Some(mutation))
  }

  def initialTestRun(): Status = {
    testFunctions(None)
  }

  @tailrec
  private def runTests(testTasks: Array[Task], status: AtomicReference[Status]): sbt.testing.Status = {
    val eventHandler = new StatusEventHandler(status)

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
    if (newTasks.nonEmpty) runTests(newTasks, status)
    else status.get()
  }

  class StatusEventHandler(status: AtomicReference[Status]) extends EventHandler {
    override def handle(event: Event) = {
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
