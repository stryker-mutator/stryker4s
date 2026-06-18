package stryker4s.mill.runner

import cats.data.NonEmptyList
import cats.effect.{Deferred, IO, Resource}
import fs2.io.file.Path
import mill.api.TaskCtx
import mill.api.daemon.Result
import mill.api.daemon.internal.CompileProblemReporter
import mill.javalib.api.{CompilationResult, JvmWorkerApi}
import stryker4s.config.source.ConfigSource
import stryker4s.config.{Config, TestFilter}
import stryker4s.exception.TestSetupFailedException
import stryker4s.extension.FileExtensions.*
import stryker4s.log.Logger
import stryker4s.model.{CompilerErrMsg, TestRunnerId}
import stryker4s.mutants.tree.InstrumenterOptions
import stryker4s.run.testrunner.ProcessTestRunner
import stryker4s.run.{Stryker4sRunner, TestRunner}
import stryker4s.testrunner.api.TestGroup

import java.io.File
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.FiniteDuration

/** All required (pre-evaluated) Mill task values needed to compile the mutated sources and run the test-runner.
  */
final case class StrykerMillContext(
    taskCtx: TaskCtx,
    worker: JvmWorkerApi,
    upstreamCompileOutput: Seq[CompilationResult],
    sourceFiles: Seq[os.Path],
    compileClasspath: Seq[os.Path],
    javaHome: Option[os.Path],
    javacOptions: Seq[String],
    scalaVersion: String,
    scalacOptions: Seq[String],
    compilerClasspath: Seq[mill.api.PathRef],
    scalacPluginClasspath: Seq[mill.api.PathRef],
    compilerBridge: Option[mill.api.PathRef],
    testRunClasspath: Seq[os.Path],
    forkArgs: Seq[String],
    testGroups: Seq[TestGroup],
    testRunnerClasspath: Seq[os.Path]
)

/** This Runner runs Stryker mutations from a Mill `stryker` command
  */
class Stryker4sMillRunner(
    ctx: StrykerMillContext,
    sharedTimeout: Deferred[IO, FiniteDuration],
    override val extraConfigSources: List[ConfigSource[IO]]
)(using log: Logger)
    extends Stryker4sRunner {

  override def resolveTestRunners(
      tmpDir: Path
  )(using config: Config): Either[NonEmptyList[CompilerErrMsg], NonEmptyList[Resource[IO, TestRunner]]] =
    compileMutatedSources(tmpDir).map { mutatedClasses =>
      val classpath = (mutatedClasses +: ctx.testRunClasspath) ++ ctx.testRunnerClasspath

      val concurrency = if config.debug.debugTestRunner then {
        log.warn(
          "'debug.debug-test-runner' config is 'true', creating 1 test-runner with debug arguments enabled on port 8000."
        )
        1
      } else {
        log.info(s"Creating ${config.concurrency} test-runners")
        config.concurrency
      }

      val testRunnerIds = NonEmptyList.fromListUnsafe(
        (1 to concurrency).map(TestRunnerId(_)).toList
      )

      val testGroups =
        if config.testFilter.isEmpty then ctx.testGroups
        else {
          val testFilter = new TestFilter()
          ctx.testGroups.map(group =>
            group.copy(taskDefs = group.taskDefs.filter(taskDef => testFilter.filter(taskDef.fullyQualifiedName)))
          )
        }

      testRunnerIds.map { id =>
        ProcessTestRunner.create(
          ctx.javaHome.map(p => new File(p.toString())),
          classpath.map(p => Path.fromNioPath(p.toNIO)),
          ctx.forkArgs,
          testGroups,
          id,
          sharedTimeout
        )
      }
    }

  /** Compile the module's sources, with sources that were copied into the mutation tmpDir replaced by their mutated
    * counterparts. Uses Mill's zinc worker with the same inputs as the module's own `compile` task.
    *
    * @return
    *   the path to the compiled classes, or the compiler errors if the mutated sources failed to compile
    */
  private def compileMutatedSources(
      tmpDir: Path
  )(using Config): Either[NonEmptyList[CompilerErrMsg], os.Path] = {
    val mutatedSources = ctx.sourceFiles.map { source =>
      val mutated = Path(source.toString()).inSubDir(tmpDir)
      // Files not matched by the 'files' config are not copied to the tmpDir, for those use the original
      if os.exists(os.Path(mutated.absolute.toNioPath)) then os.Path(mutated.absolute.toNioPath) else source
    }

    log.debug(s"Compiling ${mutatedSources.size} mutated sources to ${ctx.taskCtx.dest}")
    val reporter = new CompileErrorCollector(tmpDir)

    val result = ctx.worker.compileMixed(
      upstreamCompileOutput = ctx.upstreamCompileOutput,
      sources = mutatedSources,
      compileClasspath = ctx.compileClasspath ++ ctx.testRunnerClasspath,
      javaHome = ctx.javaHome,
      javacOptions = ctx.javacOptions,
      scalaVersion = ctx.scalaVersion,
      scalaOrganization = "org.scala-lang",
      scalacOptions = ctx.scalacOptions,
      compilerClasspath = ctx.compilerClasspath,
      scalacPluginClasspath = ctx.scalacPluginClasspath,
      compilerBridgeOpt = ctx.compilerBridge,
      reporter = Some(reporter),
      reportCachedProblems = true,
      incrementalCompilation = true,
      auxiliaryClassFileExtensions = Seq.empty,
      workDir = ctx.taskCtx.dest
    )(using ctx.taskCtx)

    result match {
      case Result.Success(compilationResult) => Right(compilationResult.classes.path)
      case failure: Result.Failure           =>
        NonEmptyList
          .fromList(reporter.errors.toList)
          .toLeft(throw TestSetupFailedException(s"Failed to compile mutated sources: ${failure.error}"))
    }
  }

  override def instrumenterOptions(using Config): InstrumenterOptions =
    InstrumenterOptions.testRunner
}

/** Collects compile errors, relativized to the mutation tmpDir, so failing mutants can be rolled back.
  */
private class CompileErrorCollector(tmpDir: Path) extends CompileProblemReporter {
  val errors = ListBuffer.empty[CompilerErrMsg]

  override def logError(problem: mill.api.daemon.internal.Problem): Unit = {
    val path = problem.position.sourceFile
      .map(f => Path.fromNioPath(f.toPath()).relativePath(tmpDir).toString)
      .getOrElse("")
    errors += CompilerErrMsg(
      msg = problem.message,
      path = path,
      line = problem.position.line.getOrElse(0),
      offset = problem.position.offset
    )
  }

  override def start(): Unit = ()
  override def logWarning(problem: mill.api.daemon.internal.Problem): Unit = ()
  override def logInfo(problem: mill.api.daemon.internal.Problem): Unit = ()
  override def fileVisited(file: java.nio.file.Path): Unit = ()
  override def printSummary(): Unit = ()
  override def finish(): Unit = ()
  override def notifyProgress(progress: Long, total: Long): Unit = ()
}
