package stryker4s.sbt.runner

import java.io.{File => JFile}
import java.nio.file.Path

import better.files.{File, _}
import sbt.Keys._
import sbt._
import stryker4s.config.{Config, TestFilter}
import stryker4s.extension.FileExtensions._
import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.run.MutantRunner
import stryker4s.extension.exception.TestSetupException

class SbtMutantRunner(state: State, sourceCollector: SourceCollector, reporter: Reporter)(implicit config: Config)
    extends MutantRunner(sourceCollector, reporter) {
  type Context = SbtRunnerContext

  /** Remove scalacOptions that are very likely to cause errors with generated code
    * https://github.com/stryker-mutator/stryker4s/issues/321
    */
  private val blacklistedScalacOptions = Seq(
    // Scala 2.12
    "-Ywarn-unused:patvars",
    "-Ywarn-unused:locals",
    "-Ywarn-unused:params",
    // Scala 2.13
    "-Wunused:patvars",
    "-Wunused:locals",
    "-Wunused:params"
  )
  def initializeTestContext(tmpDir: File): Context = {
    val settings: Seq[Def.Setting[_]] = Seq(
      scalacOptions --= blacklistedScalacOptions,
      scalaSource in Compile := tmpDirFor(Compile, tmpDir).value
    ) ++ {
      if (config.testFilter.nonEmpty) {
        val testFilter = new TestFilter
        Seq(Test / testOptions := Seq(Tests.Filter(testFilter.filter)))
      } else
        Nil
    }

    val extracted = Project.extract(state)

    val newState = extracted.appendWithSession(settings, state)
    val testGroups = Project.runTask(testGrouping in Test, newState) match {
      case Some((_, Value(groups))) => groups
      case other =>
        throw new TestSetupException(
          s"Could not setup mutation testing environment. Expected test groups, but got $other"
        )
    }
    val frameworks = (Project.runTask(loadedTestFrameworks in Test, newState) match {
      case Some((_, Value(groups))) if groups.nonEmpty => groups
      case other =>
        throw new TestSetupException(
          s"Could not setup mutation testing environment. Expected test frameworks, but got $other"
        )
    }).values.toSeq

    val classpath = Project.runTask(fullClasspath in Test, newState) match {
      case Some((_, Value(classpath))) => classpath.map(_.data.getPath())
      case other =>
        throw new TestSetupException(
          s"Could not setup mutation testing environment. Unable to resolve classpath. Expected a classpath, but got $other"
        )
    }
    val processManager = ProcessManager.newProcess(classpath)

    SbtRunnerContext(frameworks, testGroups, processManager, tmpDir)
  }

  override def runInitialTest(context: Context): Boolean =
    context.processHandler.initialTestRun(context.frameworks, context.testGroups)

  override def runMutant(mutant: Mutant, context: Context): Path => MutantRunResult =
    path => context.processHandler.runMutant(mutant, path)

  override def dispose(context: Context): Unit = {
    context.processHandler.close()
  }

  private def tmpDirFor(conf: Configuration, tmpDir: File): Def.Initialize[JFile] =
    (scalaSource in conf)(_.toScala)(source => (source inSubDir tmpDir).toJava)
}
