package stryker4s.maven

import better.files._
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker._
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.MockitoSugar
import stryker4s.config.Config
import stryker4s.extension.mutationtype.LesserThan
import stryker4s.maven.runner.MavenMutantRunner
import stryker4s.model
import stryker4s.model.{Killed, Survived}
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.testutil.Stryker4sSuite

import scala.collection.JavaConverters._
import scala.meta._

class MavenMutantRunnerTest extends Stryker4sSuite with MockitoSugar {
  implicit val config: Config = Config()
  describe("runInitialTest") {
    it("should succeed on exit-code 0 invoker") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]

      when(mockResult.getExitCode).thenReturn(0)
      when(invokerMock.execute(any)).thenReturn(mockResult)
      val sut = new MavenMutantRunner(new MavenProject(), invokerMock, mock[SourceCollector], mock[Reporter])

      val result = sut.runInitialTest(File.currentWorkingDirectory)

      verify(invokerMock).setWorkingDirectory(any)
      result should be(true)
    }

    it("should fail on exit-code 1 invoker") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(1)
      when(invokerMock.execute(any)).thenReturn(mockResult)
      val sut = new MavenMutantRunner(new MavenProject(), invokerMock, mock[SourceCollector], mock[Reporter])

      val result = sut.runInitialTest(File.currentWorkingDirectory)

      verify(invokerMock).setWorkingDirectory(any)
      result should be(false)
    }

    it("should not add the environment variable") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(0)
      when(invokerMock.execute(any)).thenReturn(mockResult)
      val captor = ArgCaptor[InvocationRequest]
      val sut = new MavenMutantRunner(new MavenProject(), invokerMock, mock[SourceCollector], mock[Reporter])

      val cwd = File.currentWorkingDirectory
      val result = sut.runInitialTest(cwd)

      result should be(true)
      verify(invokerMock).setWorkingDirectory(any)
      verify(invokerMock).execute(captor)
      val invokedRequest = captor.value
      invokedRequest.getShellEnvironments should be(empty)
    }
  }

  describe("runMutants") {
    it("should have a Killed mutant on a exit-code 1") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(1)
      when(invokerMock.execute(any)).thenReturn(mockResult)
      val sut = new MavenMutantRunner(new MavenProject(), invokerMock, mock[SourceCollector], mock[Reporter])

      val cwd = File.currentWorkingDirectory
      val result = sut.runMutant(model.Mutant(1, q">", q"<", LesserThan), cwd)(cwd.path)

      verify(invokerMock).setWorkingDirectory(any)
      result shouldBe a[Killed]
    }

    it("should have a Survived mutant on a exit-code 0") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(0)
      when(invokerMock.execute(any)).thenReturn(mockResult)
      val sut = new MavenMutantRunner(new MavenProject(), invokerMock, mock[SourceCollector], mock[Reporter])

      val cwd = File.currentWorkingDirectory
      val result = sut.runMutant(model.Mutant(1, q">", q"<", LesserThan), cwd)(cwd.path)

      verify(invokerMock).setWorkingDirectory(any)
      result shouldBe a[Survived]
    }

    it("should add the environment variable to the request") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(1)
      when(invokerMock.execute(any)).thenReturn(mockResult)
      val captor = ArgCaptor[InvocationRequest]
      val sut = new MavenMutantRunner(new MavenProject(), invokerMock, mock[SourceCollector], mock[Reporter])

      val cwd = File.currentWorkingDirectory
      sut.runMutant(model.Mutant(1, q">", q"<", LesserThan), cwd)

      verify(invokerMock).setWorkingDirectory(any)
      verify(invokerMock).execute(captor)
      val invokedRequest = captor.value
      invokedRequest.getShellEnvironments.asScala should equal(Map("ACTIVE_MUTATION" -> "1"))
      invokedRequest.getGoals should contain only "test"
      invokedRequest.isBatchMode should be(true)
      invokedRequest.getProperties.getProperty("surefire.skipAfterFailureCount") should equal("1")
    }
  }
}
