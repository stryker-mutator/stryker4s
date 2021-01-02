package stryker4s.maven

import java.io.{File => JFile}
import java.{util => ju}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.meta._

import better.files._
import cats.effect.IO
import org.apache.maven.model.Profile
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker._
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.MockitoSugar
import stryker4s.config.Config
import stryker4s.extension.mutationtype.LesserThan
import stryker4s.model.{Killed, Mutant, Survived}
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.testutil.Stryker4sSuite

class Stryker4sMavenRunnerTest extends Stryker4sSuite with MockitoSugar {
  implicit val config: Config = Config.default
  implicit val timer = IO.timer(ExecutionContext.global)

  val tmpDir = File("/home/user/tmpDir").path

  describe("resolveTestRunner") {
    it("should set the working directory") {
      val invokerMock = mock[Invoker]
      when(invokerMock.setWorkingDirectory(any[JFile])).thenReturn(null)
      val sut = new Stryker4sMavenRunner(new MavenProject(), invokerMock)

      sut
        .resolveTestRunner(tmpDir)
        .use(result => {
          verify(invokerMock).setWorkingDirectory(eqTo(tmpDir.toFile()))
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
      val sut = new Stryker4sMavenRunner(new MavenProject(), invokerMock)

      sut
        .resolveTestRunner(tmpDir)
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
      val sut = new Stryker4sMavenRunner(mavenProject, invokerMock)

      sut
        .resolveTestRunner(tmpDir)
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
      val sut = new Stryker4sMavenRunner(mavenProject, invokerMock)

      sut
        .resolveTestRunner(tmpDir)
        .use(result =>
          IO.pure(result.properties.getProperty("wildcardSuites") should equal(s"*OtherTest,$expectedTestFilter"))
        )
        .unsafeRunSync()
    }
  }

}
