package stryker4s.maven.runner

import cats.effect.IO
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker.{DefaultInvocationRequest, InvocationRequest, Invoker}
import stryker4s.log.Logger
import stryker4s.model.*
import stryker4s.run.TestRunner

import java.util.Properties
import scala.jdk.CollectionConverters.*
import mutationtesting.MutantResult
import mutationtesting.MutantStatus

class MavenTestRunner(project: MavenProject, invoker: Invoker, val properties: Properties, val goals: Seq[String])(
    implicit log: Logger
) extends TestRunner {

  def initialTestRun(): IO[InitialTestRunResult] = {
    val request = createRequest()

    IO(invoker.execute(request)).map(_.getExitCode() == 0).map(NoCoverageInitialTestRun(_))
  }

  def runMutant(mutant: MutantWithId, testNames: Seq[String]): IO[MutantResult] = {
    val request = createRequestWithMutation(mutant.id)

    IO(invoker.execute(request)).map { result =>
      result.getExitCode match {
        case 0 => mutant.toMutantResult(MutantStatus.Survived)
        case _ => mutant.toMutantResult(MutantStatus.Killed)
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

  private def createRequestWithMutation(mutant: MutantId): InvocationRequest =
    createRequest()
      .addShellEnvironment("ACTIVE_MUTATION", mutant.globalId.toString())

}
