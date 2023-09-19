package stryker4s.maven

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.io.file.Path
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker.*
import org.mockito.scalatest.MockitoSugar
import stryker4s.config.Config
import stryker4s.testutil.Stryker4sSuite

class Stryker4sMavenRunnerTest extends Stryker4sSuite with MockitoSugar {
  implicit val config: Config = Config.default

  val tmpDir = Path("/home/user/tmpDir")

  describe("resolveTestRunner") {

    it("should add test-filter for all test runners") {
      val expectedTestFilter = Seq("*MavenMutantRunnerTest", "*OtherTest")
      implicit val config: Config = Config.default.copy(testFilter = expectedTestFilter)
      val invokerMock = mock[Invoker]
      val sut = new Stryker4sMavenRunner(new MavenProject(), invokerMock)

      sut
        .resolveTestRunners(tmpDir)
        .toOption
        .get
        .head
        .use(result => {
          result.goals should contain only "test"
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
