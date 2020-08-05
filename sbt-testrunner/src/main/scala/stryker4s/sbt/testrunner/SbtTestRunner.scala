package stryker4s.sbt.testrunner

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator

import scala.annotation.tailrec

import sbt.testing.{Event, EventHandler, Framework, Status, Task, TaskDef}
import stryker4s.api.testprocess._

sealed trait TestRunner {
  def runMutation(mutation: Int): Status
  def initialTestRun(): Status
}

class SbtTestRunner(context: TestProcessContext) extends TestRunner {

  def testFunctions(): Option[Int] => Status = {
    val cl = getClass().getClassLoader()
    val tasks = context.testGroups.map(testGroup => {
      val RunnerOptions(args, remoteArgs) = testGroup.runnerOptions
      val framework = cl.loadClass(testGroup.frameworkClass).getConstructor().newInstance().asInstanceOf[Framework]
      val runner = framework.runner(args, remoteArgs, cl)
      runner.tasks(testGroup.taskDefs.map(toSbtTaskDef))
    })

    (mutation: Option[Int]) => {
      tasks.foldLeft(Status.Success)({
        case (accResult, tasksToRun) =>
          // Fail early
          accResult match {
            case Status.Failure => accResult
            case Status.Error   => accResult
            case _ =>
              mutation.foreach(activateMutation)
              val result = runTests(tasksToRun, new AtomicReference(Status.Success))

              combineStatus(accResult, result)
          }
      })
    }
  }
  val fs = testFunctions()

  def runMutation(mutation: Int) = {
    fs(Some(mutation))
  }

  def initialTestRun(): Status = {
    fs(None)
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

  // TODO: Move all below to somewhere else
  def combineStatus(current: Status, newStatus: Status) =
    (current, newStatus) match {
      case (Status.Error, _)   => Status.Error
      case (_, Status.Error)   => Status.Error
      case (Status.Failure, _) => Status.Failure
      case (_, Status.Failure) => Status.Failure
      case _                   => Status.Success
    }

  def toSbtTaskDef(td: TaskDefinition) = {
    val fingerprint = toSbtFingerprint(td.fingerprint)
    val selectors = td.selectors.map(toSbtSelector)
    new TaskDef(td.fullyQualifiedName, fingerprint, td.explicitlySpecified, selectors)
  }

  def toSbtSelector(s: Selector): sbt.testing.Selector =
    s match {
      case NestedSuiteSelector(suiteId)          => new sbt.testing.NestedSuiteSelector(suiteId)
      case NestedTestSelector(suiteId, testName) => new sbt.testing.NestedTestSelector(suiteId, testName)
      case SuiteSelector()                       => new sbt.testing.SuiteSelector()
      case TestSelector(testName)                => new sbt.testing.TestSelector(testName)
      case TestWildcardSelector(testWildcard)    => new sbt.testing.TestWildcardSelector(testWildcard)
    }

  def toSbtFingerprint(f: Fingerprint): sbt.testing.Fingerprint =
    f match {
      case AnnotatedFingerprint(fIsModule, annotation) =>
        new sbt.testing.AnnotatedFingerprint() {
          def isModule(): Boolean = fIsModule
          def annotationName(): String = annotation
        }
      case SubclassFingerprint(fIsModule, superclass, noArgs) =>
        new sbt.testing.SubclassFingerprint() {
          def isModule(): Boolean = fIsModule

          def superclassName(): String = superclass

          def requireNoArgConstructor(): Boolean = noArgs

        }
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
