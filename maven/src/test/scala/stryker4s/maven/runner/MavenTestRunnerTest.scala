package stryker4s.maven.runner

import cats.syntax.option.*
import fs2.io.file.Path
import mutationtesting.{Location, MutantStatus, Position}
import org.apache.maven.model.Profile
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker.{InvocationRequest, InvocationResult, Invoker}
import org.mockito.captor.ArgCaptor
import stryker4s.config.Config
import stryker4s.mutation.LesserThan
import stryker4s.model.{MutantId, MutantMetadata, MutantWithId, MutatedCode, NoCoverageInitialTestRun}
import stryker4s.testkit.{LogMatchers, MockitoSuite, Stryker4sIOSuite}

import java.util as ju
import scala.jdk.CollectionConverters.*
import scala.meta.*

class MavenTestRunnerTest extends Stryker4sIOSuite with MockitoSuite with LogMatchers {
  implicit val config: Config = Config.default

  val tmpDir = Path("/home/user/tmpDir")
  val coverageTestNames = Seq.empty[String]
  def properties = new ju.Properties()
  def goals = Seq("test")

  describe("runInitialTest") {

    test("should fail on exit-code 1 invoker") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(1)
      when(invokerMock.execute(any[InvocationRequest])).thenReturn(mockResult)
      val sut = new MavenTestRunner(new MavenProject(), invokerMock, properties, goals, tmpDir)

      sut.initialTestRun().assertEquals(NoCoverageInitialTestRun(false))
    }

    test("should not add the environment variable") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(0)
      when(invokerMock.execute(any[InvocationRequest])).thenReturn(mockResult)
      val captor = ArgCaptor[InvocationRequest]
      val sut = new MavenTestRunner(new MavenProject(), invokerMock, properties, goals, tmpDir)

      sut.initialTestRun().asserting { result =>
        assertEquals(result, NoCoverageInitialTestRun(true))
        verify(invokerMock).execute(captor)
        val invokedRequest = captor.value
        assert(invokedRequest.getShellEnvironments.isEmpty)
      }
    }

    test("should propagate active profiles") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(0)
      when(invokerMock.execute(any[InvocationRequest])).thenReturn(mockResult)
      val captor = ArgCaptor[InvocationRequest]
      val mavenProject = new MavenProject()
      val profile = new Profile()
      profile.setId("best-profile-ever")
      mavenProject.getActiveProfiles.add(profile)
      val sut = new MavenTestRunner(mavenProject, invokerMock, properties, goals, tmpDir)

      sut.initialTestRun().asserting { _ =>
        verify(invokerMock).execute(captor)
        val invokedRequest = captor.value
        assert(invokedRequest.getProfiles.contains("best-profile-ever"))
      }
    }
  }

  describe("runMutants") {
    test("should have a Killed mutant on a exit-code 1") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(1)
      when(invokerMock.execute(any[InvocationRequest])).thenReturn(mockResult)
      val sut = new MavenTestRunner(new MavenProject(), invokerMock, properties, goals, tmpDir)

      sut.runMutant(createMutant, coverageTestNames).map(_.status).assertEquals(MutantStatus.Killed)
    }

    test("should have a Survived mutant on a exit-code 0") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(0)
      when(invokerMock.execute(any[InvocationRequest])).thenReturn(mockResult)
      val sut = new MavenTestRunner(new MavenProject(), invokerMock, properties, goals, tmpDir)

      sut.runMutant(createMutant, coverageTestNames).map(_.status).assertEquals(MutantStatus.Survived)
    }

    test("should add the environment variable to the request") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(1)
      when(invokerMock.execute(any[InvocationRequest])).thenReturn(mockResult)
      val captor = ArgCaptor[InvocationRequest]
      val project = new MavenProject()
      project.getProperties().setProperty("surefire.skipAfterFailureCount", "1")

      val sut = new MavenTestRunner(project, invokerMock, project.getProperties(), goals, tmpDir)

      sut.runMutant(createMutant, coverageTestNames).asserting { _ =>
        verify(invokerMock).execute(captor)
        val invokedRequest = captor.value
        assertEquals(invokedRequest.getShellEnvironments.asScala.toMap, Map("ACTIVE_MUTATION" -> "1"))
        assertEquals(invokedRequest.getGoals.asScala.toList, List("test"))
        assert(invokedRequest.isBatchMode)
        assertEquals(invokedRequest.getProperties.getProperty("surefire.skipAfterFailureCount"), "1")
        assertEquals(invokedRequest.getProperties.getProperty("test"), null)
        assertEquals(invokedRequest.getBaseDirectory(), tmpDir.toNioPath.toFile())
      }
    }

    test("should propagate active profiles") {
      val invokerMock = mock[Invoker]
      val mockResult = mock[InvocationResult]
      when(mockResult.getExitCode).thenReturn(0)
      when(invokerMock.execute(any[InvocationRequest])).thenReturn(mockResult)
      val captor = ArgCaptor[InvocationRequest]
      val mavenProject = new MavenProject()
      val profile = new Profile()
      profile.setId("best-profile-ever")
      mavenProject.getActiveProfiles.add(profile)
      val sut = new MavenTestRunner(mavenProject, invokerMock, properties, goals, tmpDir)

      sut
        .runMutant(createMutant, coverageTestNames)
        .map { _ =>
          verify(invokerMock).execute(captor)
          val invokedRequest = captor.value
          assert(invokedRequest.getProfiles.contains("best-profile-ever"))
        }
    }
  }

  def createMutant =
    MutantWithId(
      MutantId(1),
      MutatedCode(q"<", MutantMetadata(">", "<", LesserThan.mutationName, createLocation, none))
    )

  def createLocation = Location(Position(0, 0), Position(0, 0))
}
