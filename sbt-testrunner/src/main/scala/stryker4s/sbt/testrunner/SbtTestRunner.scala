package stryker4s.sbt.testrunner

import sbt.testing.EventHandler
import sbt.testing.Task
import scala.annotation.tailrec
import sbt.testing.Status
import sbt.testing.Framework

class SbtTestRunner(context: Context) {

  def testMutations(mutations: Seq[Int]): Unit = {
    val RunnerOptions(args, remoteArgs) = context.runnerOptions
    val cl = getClass().getClassLoader()
    val framework = cl.loadClass(context.frameworkClass).getConstructor().newInstance().asInstanceOf[Framework]
    val runner = framework.runner(args, remoteArgs, cl)
    val taskDefs = runner.tasks(context.taskDefs)

    mutations.foreach(mutation => {
      activateMutation(mutation)
      runMutation(taskDefs)
    })

    val _ = runner.done()
  }

  private def activateMutation(mutation: Int): Unit = {
    val _ = sys.props += (("ACTIVE_MUTATION", String.valueOf(mutation)))
  }

  @tailrec
  private def runMutation(testTasks: Array[Task], acc: Status = Status.Pending): sbt.testing.Status = {
    var status = acc
    val eventHandler: EventHandler = event => {
      status = combineStatus(status, event.status())
    }
    val newTasks = testTasks.flatMap(task => task.execute(eventHandler, Array.empty))
    if (newTasks.nonEmpty) runMutation(newTasks, status)
    else status
  }

  def combineStatus(current: Status, newStatus: Status) =
    (current, newStatus) match {
      case (Status.Error, _)   => Status.Error
      case (_, Status.Error)   => Status.Error
      case (Status.Failure, _) => Status.Failure
      case (_, Status.Failure) => Status.Failure
      case _                   => Status.Success
    }
}
