package stryker4s.maven.runner

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.meta._

import better.files._
import cats.effect.IO
import java.io.{File => JFile}
import java.{util => ju}
import org.apache.maven.model.Profile
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker._
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.MockitoSugar
import stryker4s.config.Config
import stryker4s.extension.mutationtype.LesserThan
import stryker4s.model.{Killed, MavenRunnerContext, Mutant, Survived}
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.testutil.Stryker4sSuite

class MavenMutantRunnerTest extends Stryker4sSuite with MockitoSugar {
  implicit val config: Config = Config.default
  implicit val timer = IO.timer(ExecutionContext.global)
  def context = MavenRunnerContext(new ju.Properties(), Seq("test"))
  val tmpDir = File("/home/user/tmpDir")

  describe("initializeTestContext") {
    it("should set the working directory") {
      val invokerMock = mock[Invoker]
      when(invokerMock.setWorkingDirectory(any[JFile])).thenReturn(null)
      val sut = new MavenMutantRunner(new MavenProject(), invokerMock, mock[SourceCollector], mock[Reporter])

      sut
        .initializeTestContext(tmpDir)
        .use(result => {
          verify(invokerMock).setWorkingDirectory(eqTo(tmpDir.toJava))
          result.goals should contain only "test"
          IO.unit
        })
        .unsafeRunSync()
    }

    it("should add test-filter for all test runners") {
      val expectedTestFilter = Seq("*MavenMutantRunnerTest", "*OtherTest")
      implicit val config: Config = Config.default.copy(testFilter = expectedTestFilter)
      val invokerMock = mock[Invoker]
      when(invokerMock.setWorkingDirectory(any[JFile])).thenReturn(null)
      val sut = new MavenMutantRunner(new MavenProject(), invokerMock, mock[SourceCollector], mock[Reporter])

      sut
        .initializeTestContext(tmpDir)
        .use(result => {
          result.properties.getProperty("test") should equal(expectedTestFilter.mkString(", "))
          result.properties.getProperty("wildcardSuites") should equal(expectedTestFilter.mkString(","))
          IO.unit
        })
        .unsafeRunSync()
    }

    it("should add test-filter for surefire if a property is already defined") {
      val expectedTestFilter = "*MavenMutantRunnerTest"
      implicit val config: Config = Config.default.copy(testFilter = Seq(expectedTestFilter))
      val invokerMock = mock[Invoker]
      when(invokerMock.setWorkingDirectory(any[JFile])).thenReturn(null)
      val mavenProject = new MavenProject()
      mavenProject.getProperties().setProperty("test", "*OtherTest")
      val sut = new MavenMutantRunner(mavenProject, invokerMock, mock[SourceCollector], mock[Reporter])

      sut
        .initializeTestContext(tmpDir)
        .use(result => IO.pure(result.properties.getProperty("test") should equal(s"*OtherTest, $expectedTestFilter")))
        .unsafeRunSync()
    }

    it("should add test-filter for scalatest if a property is already defined") {
      val expectedTestFilter = "*MavenMutantRunnerTest"
      implicit val config: Config = Config.default.copy(testFilter = Seq(expectedTestFilter))
      val invokerMock = mock[Invoker]
      when(invokerMock.setWorkingDirectory(any[JFile])).thenReturn(null)
      val mavenProject = new MavenProject()
      mavenProject.getProperties().setProperty("wildcardSuites", "*OtherTest")
      val sut = new MavenMutantRunner(mavenProject, invokerMock, mock[SourceCollector], mock[Reporter])

      sut
        .initializeTestContext(tmpDir)
        .use(result =>
          IO.pure(result.properties.getProperty("wildcardSuites") should equal(s"*OtherTest,$expectedTestFilter"))
        )
        .unsafeRunSync()
    }
  }
  describe("runInitialTest") {

    it("should fail on exit-code 1 invoker") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(1)
      when(invokerMock.execute(any[InvocationRequest])).thenReturn(mockResult)
      val sut = new MavenMutantRunner(new MavenProject(), invokerMock, mock[SourceCollector], mock[Reporter])

      val result = sut.runInitialTest(context).unsafeRunSync()

      result should be(false)
    }

    it("should not add the environment variable") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(0)
      when(invokerMock.execute(any[InvocationRequest])).thenReturn(mockResult)
      val captor = ArgCaptor[InvocationRequest]
      val sut = new MavenMutantRunner(new MavenProject(), invokerMock, mock[SourceCollector], mock[Reporter])

      val result = sut.runInitialTest(context).unsafeRunSync()

      result should be(true)
      verify(invokerMock).execute(captor)
      val invokedRequest = captor.value
      invokedRequest.getShellEnvironments should be(empty)
    }

    it("should propagate active profiles") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(0)
      when(invokerMock.execute(any[InvocationRequest])).thenReturn(mockResult)
      val captor = ArgCaptor[InvocationRequest]
      val mavenProject = new MavenProject()
      val profile = new Profile()
      profile.setId("best-profile-ever")
      mavenProject.getActiveProfiles.add(profile)
      val sut = new MavenMutantRunner(mavenProject, invokerMock, mock[SourceCollector], mock[Reporter])

      sut.runInitialTest(context).unsafeRunSync()

      verify(invokerMock).execute(captor)
      val invokedRequest = captor.value
      invokedRequest.getProfiles.asScala should contain("best-profile-ever")
    }
  }

  describe("runMutants") {
    it("should have a Killed mutant on a exit-code 1") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(1)
      when(invokerMock.execute(any[InvocationRequest])).thenReturn(mockResult)
      val sut = new MavenMutantRunner(new MavenProject(), invokerMock, mock[SourceCollector], mock[Reporter])

      val result = sut.runMutant(Mutant(1, q">", q"<", LesserThan), context).unsafeRunSync()

      result shouldBe a[Killed]
    }

    it("should have a Survived mutant on a exit-code 0") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(0)
      when(invokerMock.execute(any[InvocationRequest])).thenReturn(mockResult)
      val sut = new MavenMutantRunner(new MavenProject(), invokerMock, mock[SourceCollector], mock[Reporter])

      val result = sut.runMutant(Mutant(1, q">", q"<", LesserThan), context).unsafeRunSync()

      result shouldBe a[Survived]
    }

    it("should add the environment variable to the request") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(1)
      when(invokerMock.execute(any[InvocationRequest])).thenReturn(mockResult)
      val captor = ArgCaptor[InvocationRequest]
      val project = new MavenProject()
      project.getProperties().setProperty("surefire.skipAfterFailureCount", "1")
      val newContext = context.copy(properties = project.getProperties())
      val sut = new MavenMutantRunner(project, invokerMock, mock[SourceCollector], mock[Reporter])

      sut.runMutant(Mutant(1, q">", q"<", LesserThan), newContext).unsafeRunSync()

      verify(invokerMock).execute(captor)
      val invokedRequest = captor.value
      invokedRequest.getShellEnvironments.asScala should equal(Map("ACTIVE_MUTATION" -> "1"))
      invokedRequest.getGoals should contain only "test"
      invokedRequest.isBatchMode should be(true)
      invokedRequest.getProperties.getProperty("surefire.skipAfterFailureCount") should equal("1")
      invokedRequest.getProperties.getProperty("test") shouldBe null
    }

    it("should propagate active profiles") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(0)
      when(invokerMock.execute(any[InvocationRequest])).thenReturn(mockResult)
      val captor = ArgCaptor[InvocationRequest]
      val mavenProject = new MavenProject()
      val profile = new Profile()
      profile.setId("best-profile-ever")
      mavenProject.getActiveProfiles.add(profile)
      val sut = new MavenMutantRunner(mavenProject, invokerMock, mock[SourceCollector], mock[Reporter])

      sut.runMutant(Mutant(1, q">", q"<", LesserThan), context).unsafeRunSync()

      verify(invokerMock).execute(captor)
      val invokedRequest = captor.value
      invokedRequest.getProfiles.asScala should contain("best-profile-ever")
    }
  }
}
