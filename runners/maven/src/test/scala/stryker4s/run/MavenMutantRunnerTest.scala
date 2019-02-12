package stryker4s.run

import java.nio.file.Paths

import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker._
import stryker4s.config.Config
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.process.ProcessRunner
import stryker4s.testutil.Stryker4sSuite
import better.files._
import org.mockito.captor.ArgCaptor
import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito, MockitoSugar}
import stryker4s.extension.mutationtype.Or
import stryker4s.model
import stryker4s.model.{Killed, Mutant}

import scala.meta._
import scala.collection.JavaConverters._
class MavenMutantRunnerTest extends Stryker4sSuite with IdiomaticMockito with ArgumentMatchersSugar  {
  implicit val config: Config = Config()
  describe("runInitialTest") {
    it("should succeed on exit-code 0 invoker") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]

      0 willBe returned by mockResult.getExitCode
      mockResult willBe returned by invokerMock.execute(*)
      val sut = new MavenMutantRunner(new MavenProject(), invokerMock, mock[ProcessRunner], mock[SourceCollector])

      val result = sut.runInitialTest(File.currentWorkingDirectory)

      result should be (true)
    }

    it("should fail on exit-code 1 invoker") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      1 willBe returned by mockResult.getExitCode
      mockResult willBe returned by invokerMock.execute(*)
      val sut = new MavenMutantRunner(new MavenProject(), invokerMock, mock[ProcessRunner], mock[SourceCollector])

      val result = sut.runInitialTest(File.currentWorkingDirectory)

      result should be (false)
    }
  }

  describe("runMutants") {
    it("should add the environment variable to the request") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      1 willBe returned by mockResult.getExitCode
      mockResult willBe returned by invokerMock.execute(*)
      val captor = ArgCaptor[InvocationRequest]
      val sut = new MavenMutantRunner(new MavenProject(), invokerMock, mock[ProcessRunner], mock[SourceCollector])

      val cwd = File.currentWorkingDirectory
      val result = sut.runMutant(model.Mutant(1, q">", q"<", Or), cwd, Paths.get("stryker4sFolder"))

      invokerMock.execute(captor) was called
      result shouldBe a[Killed]
      val invokedRequest = captor.value
      invokedRequest.getShellEnvironments.asScala should equal (Map("ACTIVE_MUTATION" -> "1"))
    }
  }
}
