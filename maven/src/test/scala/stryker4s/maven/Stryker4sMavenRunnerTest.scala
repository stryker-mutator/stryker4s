package stryker4s.maven

import cats.effect.IO
import fs2.io.file.Path
import org.apache.maven.project.MavenProject
import stryker4s.config.Config
import stryker4s.files.GlobFileResolver
import stryker4s.maven.files.MavenMutatesResolver
import stryker4s.maven.stubs.InvokerStub
import stryker4s.testkit.{LogMatchers, Stryker4sIOSuite}

class Stryker4sMavenRunnerTest extends Stryker4sIOSuite with LogMatchers {

  implicit val config: Config = Config.default

  val tmpDir = Path("/home/user/tmpDir")

  describe("resolveTestRunner") {

    test("should add test-filter for all test runners") {
      val expectedTestFilter = Seq("*MavenMutantRunnerTest", "*OtherTest")
      implicit val config: Config = Config.default.copy(testFilter = expectedTestFilter)
      val invokerStub = InvokerStub()
      val sut = new Stryker4sMavenRunner(new MavenProject(), invokerStub)

      sut
        .resolveTestRunners(tmpDir)
        .toOption
        .value
        .head
        .use(result =>
          IO {
            assertEquals(result.goals, List("test"))
            assertEquals(result.properties.getProperty("test"), expectedTestFilter.mkString(", "))
            assertEquals(result.properties.getProperty("wildcardSuites"), expectedTestFilter.mkString(","))
          }
        )
        .assert
    }

    test("should add test-filter for surefire if a property is already defined") {
      val expectedTestFilter = "*MavenMutantRunnerTest"
      implicit val config: Config = Config.default.copy(testFilter = Seq(expectedTestFilter))
      val invokerStub = InvokerStub()
      val mavenProject = new MavenProject()
      mavenProject.getProperties().setProperty("test", "*OtherTest")
      val sut = new Stryker4sMavenRunner(mavenProject, invokerStub)

      sut
        .resolveTestRunners(tmpDir)
        .toOption
        .value
        .head
        .use(result => IO(assertEquals(result.properties.getProperty("test"), s"*OtherTest, $expectedTestFilter")))
        .assert
    }

    test("should add test-filter for scalatest if a property is already defined") {
      val expectedTestFilter = "*MavenMutantRunnerTest"
      implicit val config: Config = Config.default.copy(testFilter = Seq(expectedTestFilter))
      val invokerStub = InvokerStub()
      val mavenProject = new MavenProject()
      mavenProject.getProperties().setProperty("wildcardSuites", "*OtherTest")
      val sut = new Stryker4sMavenRunner(mavenProject, invokerStub)

      sut
        .resolveTestRunners(tmpDir)
        .toOption
        .value
        .head
        .use(result =>
          IO(assertEquals(result.properties.getProperty("wildcardSuites"), s"*OtherTest,$expectedTestFilter"))
        )
        .assert
    }
  }

  describe("resolveMutatesFileSource") {
    test("should resolve to a maven mutates file resolver when config.mutate is empty") {
      implicit val config = Config.default.copy(mutate = Seq.empty)
      val project = new MavenProject()
      val sut = new Stryker4sMavenRunner(project, InvokerStub())

      assert(clue(sut.resolveMutatesFileSource).isInstanceOf[MavenMutatesResolver])
    }

    test("uses glob for filled config.mutate") {
      implicit val config = Config.default.copy(mutate = Seq("src/test/resources/mutate/*"))
      val project = new MavenProject()
      val sut = new Stryker4sMavenRunner(project, InvokerStub())

      assert(clue(sut.resolveMutatesFileSource).isInstanceOf[GlobFileResolver])
    }
  }
}
