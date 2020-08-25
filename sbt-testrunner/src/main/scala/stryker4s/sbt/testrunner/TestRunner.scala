package stryker4s.sbt.testrunner

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator

import scala.annotation.tailrec

import sbt.testing.{Event, EventHandler, Framework, Status, Task}
import stryker4s.api.testprocess._

sealed trait TestRunner {
  def runMutation(mutation: Int): Status
  def initialTestRun(): Status
}

class SbtTestInterfaceRunner(context: TestProcessContext) extends TestRunner with TestInterfaceMapper {

  val testFunctions: Option[Int] => Status = {
    val cl = getClass().getClassLoader()
    val tasks = context.testGroups.flatMap(testGroup => {
      val RunnerOptions(args, remoteArgs) = testGroup.runnerOptions
      val framework = cl.loadClass(testGroup.frameworkClass).getConstructor().newInstance().asInstanceOf[Framework]
      val runner = framework.runner(args, remoteArgs, cl)
      runner.tasks(testGroup.taskDefs.map(toSbtTaskDef))
    })

    (mutation: Option[Int]) => {
      mutation.foreach(activateMutation)
      runTests(tasks, new AtomicReference(Status.Success))
    }
  }

  def runMutation(mutation: Int) = {
    testFunctions(Some(mutation))
  }

  def initialTestRun(): Status = {
    testFunctions(None)
  }

  private def activateMutation(mutation: Int) = {
    sys.props += (("ACTIVE_MUTATION", String.valueOf(mutation)))
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
          task.execute(eventHandler, Array.empty)
      }
    )
    if (newTasks.nonEmpty) runTests(newTasks, status)
    else status.get()
  }

  class StatusEventHandler(status: AtomicReference[Status]) extends EventHandler {
    override def handle(event: Event) = {
      status.updateAndGet(new UnaryOperator[Status]() {
        override def apply(old: Status) = {
          combineStatus(old, event.status())
        }
      })
      ()
    }
  }
}
