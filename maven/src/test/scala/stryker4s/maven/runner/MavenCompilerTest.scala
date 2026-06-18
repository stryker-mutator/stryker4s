package stryker4s.maven.runner

import fs2.io.file.Path
import org.apache.maven.model.Resource
import org.apache.maven.project.MavenProject
import stryker4s.testkit.Stryker4sSuite

class MavenCompilerTest extends Stryker4sSuite {

  test("isBlocklistedScalacOption blocks the exact warning-as-error and discard/dead-code options") {
    val blocked = Seq(
      "-Xfatal-warnings",
      "-Werror",
      "-Ycheck-all-patmat",
      "-Wdead-code",
      "-Ywarn-dead-code",
      "-Wvalue-discard",
      "-Ywarn-value-discard"
    )
    blocked.foreach(option =>
      assert(MavenCompiler.isBlocklistedScalacOption(option), s"expected $option to be blocked")
    )
  }

  test("isBlocklistedScalacOption blocks -Wunused and -Ywarn-unused with any suffix") {
    assert(MavenCompiler.isBlocklistedScalacOption("-Wunused"))
    assert(MavenCompiler.isBlocklistedScalacOption("-Wunused:imports"))
    assert(MavenCompiler.isBlocklistedScalacOption("-Ywarn-unused"))
    assert(MavenCompiler.isBlocklistedScalacOption("-Ywarn-unused:imports"))
  }

  test("isBlocklistedScalacOption does not block unrelated options") {
    val allowed = Seq("-deprecation", "-feature", "-Xsource:3", "-encoding", "UTF-8", "", "-Wconf")
    allowed.foreach(option =>
      assert(!MavenCompiler.isBlocklistedScalacOption(option), s"expected $option to be allowed")
    )
  }

  test("isBlocklistedScalacOption matches the blocked options exactly, not as prefixes") {
    // Guards the `==` checks against being loosened to startsWith-style matching.
    assert(!MavenCompiler.isBlocklistedScalacOption("-Werror-extra"))
    assert(!MavenCompiler.isBlocklistedScalacOption("-Xfatal-warnings-ish"))
  }

  test("isScalaOrJavaSource accepts .scala and .java files") {
    assert(MavenCompiler.isScalaOrJavaSource(Path("src/main/scala/Foo.scala")))
    assert(MavenCompiler.isScalaOrJavaSource(Path("src/main/java/Foo.java")))
  }

  test("isScalaOrJavaSource rejects other files") {
    assert(!MavenCompiler.isScalaOrJavaSource(Path("src/main/resources/application.conf")))
    assert(!MavenCompiler.isScalaOrJavaSource(Path("Foo.class")))
    assert(!MavenCompiler.isScalaOrJavaSource(Path("README.md")))
  }

  test("originalOutputs returns the build's output and test-output directories") {
    val project = new MavenProject()
    project.getBuild().setOutputDirectory("target/classes")
    project.getBuild().setTestOutputDirectory("target/test-classes")
    assertEquals(MavenCompiler.originalOutputs(project), Set("target/classes", "target/test-classes"))
  }

  test("isOriginalOutput matches the build's output dirs and nothing else") {
    val project = new MavenProject()
    project.getBuild().setOutputDirectory("target/classes")
    project.getBuild().setTestOutputDirectory("target/test-classes")
    assert(MavenCompiler.isOriginalOutput(project)(Path("target/classes")))
    assert(MavenCompiler.isOriginalOutput(project)(Path("target/test-classes")))
    assert(!MavenCompiler.isOriginalOutput(project)(Path("target/lib/some-dep.jar")))
  }

  test("resourceDirs returns the main and test resource source directories") {
    val project = new MavenProject()
    val mainRes = new Resource()
    mainRes.setDirectory("src/main/resources")
    val testRes = new Resource()
    testRes.setDirectory("src/test/resources")
    project.getBuild().addResource(mainRes)
    project.getBuild().addTestResource(testRes)
    assertEquals(MavenCompiler.resourceDirs(project), Seq(Path("src/main/resources"), Path("src/test/resources")))
  }
}
