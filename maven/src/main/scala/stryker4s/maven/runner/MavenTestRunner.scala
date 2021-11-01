package stryker4s.maven.runner

import cats.effect.IO
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker.{DefaultInvocationRequest, InvocationRequest, Invoker}
import stryker4s.api.testprocess._
import stryker4s.log.Logger
import stryker4s.model._
import stryker4s.run.TestRunner

import java.util.Properties
import scala.collection.JavaConverters._

class MavenTestRunner(project: MavenProject, invoker: Invoker, val properties: Properties, val goals: Seq[String])(
    implicit log: Logger
) extends TestRunner {

  def initialTestRun(): IO[InitialTestRunResult] = {
    val request = createRequest()

    IO(invoker.execute(request)).map(_.getExitCode() == 0).map(NoCoverageInitialTestRun(_))
  }

  def runMutant(mutant: Mutant, testNames: Seq[String]): IO[MutantRunResult] = {
    val request = createRequestWithMutation(mutant)

    IO(invoker.execute(request)).map { result =>
      result.getExitCode match {
        case 0 => Survived(mutant)
        case _ => Killed(mutant)
      }
    }
  }

  private def createRequest(): InvocationRequest =
    new DefaultInvocationRequest()
      .setGoals(goals.asJava)
      .setOutputHandler(log.debug(_))
      .setBatchMode(true)
      .setProperties(properties)
      .setProfiles(project.getActiveProfiles.asScala.map(_.getId).asJava)

  private def createRequestWithMutation(mutant: Mutant): InvocationRequest =
    createRequest()
      .addShellEnvironment("ACTIVE_MUTATION", String.valueOf(mutant.id))

}
