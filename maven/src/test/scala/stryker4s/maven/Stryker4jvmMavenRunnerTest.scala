package stryker4s.maven

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.io.file.Path
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker.*
import org.mockito.scalatest.MockitoSugar
import stryker4jvm.config.Config
import stryker4s.testutil.Stryker4jvmSuite

import java.io.File as JFile

class Stryker4jvmMavenRunnerTest extends Stryker4jvmSuite with MockitoSugar {
  implicit val config: Config = Config.default

  val tmpDir = Path("/home/user/tmpDir")

  describe("resolveTestRunner") {
    it("should set the working directory") {
      val invokerMock = mock[Invoker]
      when(invokerMock.setWorkingDirectory(any[JFile])).thenReturn(null)
      val sut = new Stryker4sMavenRunner(new MavenProject(), invokerMock)

      sut
        .resolveTestRunners(tmpDir)
        .toOption
        .get
        .head
        .use(result => {
          verify(invokerMock).setWorkingDirectory(eqTo(tmpDir.toNioPath.toFile()))
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
        .resolveTestRunners(tmpDir)
        .toOption
        .get
        .head
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
        .resolveTestRunners(tmpDir)
        .toOption
        .get
        .head
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
        .resolveTestRunners(tmpDir)
        .toOption
        .get
        .head
        .use(result =>
          IO.pure(result.properties.getProperty("wildcardSuites") should equal(s"*OtherTest,$expectedTestFilter"))
        )
        .unsafeRunSync()
    }
  }

}
