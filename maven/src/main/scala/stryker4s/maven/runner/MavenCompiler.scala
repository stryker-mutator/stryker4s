package stryker4s.maven.runner

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import org.apache.maven.project.MavenProject
import stryker4s.config.Config
import stryker4s.exception.TestSetupFailedException
import stryker4s.extension.FileExtensions.*
import stryker4s.log.Logger
import stryker4s.model.CompilerErrMsg
import xsbti.Problem

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

/** Compiles the project's mutated sources once, via zinc, for the test-runner process to load
  *
  * Compilation happens in two passes: the mutated main sources (whose compiler errors are returned so individual
  * non-compiling mutants can be rolled back), then the unchanged test sources against the freshly-compiled mutated
  * classes.
  */
class MavenCompiler(project: MavenProject, zincR: Resource[IO, ZincCompiler])(using log: Logger) {

  /** Compile the mutated sources. The returned `mainClasses`/`testClasses` dirs go on the test-runner classpath.
    *
    * @return
    *   the compiled output directories, or the compiler errors from the mutated main sources
    */
  def compile(
      tmpDir: Path,
      testRunnerClasspath: Seq[Path]
  )(using Config): IO[Either[NonEmptyList[CompilerErrMsg], MavenCompiler.Output]] = {
    val mainOut = tmpDir / "classes"
    val testOut = tmpDir / "test-classes"
    zincR.use: compiler =>
      for {
        _ <- (Files[IO].createDirectories(mainOut), Files[IO].createDirectories(testOut)).parTupled

        mainSources <- sourceFiles(project.getCompileSourceRoots().asScala.toSeq, tmpDir)
        mainClasspath = testRunnerClasspath ++
          project
            .getCompileClasspathElements()
            .asScala
            .toSeq
            .map(Path(_))
            .filterNot(MavenCompiler.isOriginalOutput(project))

        _ = log.debug(s"Compiling ${mainSources.size} mutated main sources to $mainOut")
        mainErrors <- compiler.compile(mainSources, mainClasspath, mainOut, scalacOptions, cacheFile(tmpDir, "main"))

        result <- NonEmptyList.fromList(mainErrors.flatMap(toCompilerErrMsg(_, tmpDir)).toList) match
          case Some(errors) => IO.pure(errors.asLeft)
          case None         =>
            for {
              testSources <- sourceFiles(project.getTestCompileSourceRoots().asScala.toSeq, tmpDir)
              testClasspath =
                (mainOut +: testRunnerClasspath) ++
                  project
                    .getTestClasspathElements()
                    .asScala
                    .toSeq
                    .map(Path(_))
                    .filterNot(MavenCompiler.isOriginalOutput(project))

              _ = log.debug(s"Compiling ${testSources.size} test sources to $testOut")
              testErrors <- compiler.compile(
                testSources,
                testClasspath,
                testOut,
                scalacOptions,
                cacheFile(tmpDir, "test")
              )
              _ <- IO.raiseWhen(testErrors.nonEmpty)(
                TestSetupFailedException(
                  s"Test sources failed to compile against the mutated classes:\n${testErrors.map(_.toString()).mkString("\n")}"
                )
              )
            } yield MavenCompiler.Output(mainOut, testOut).asRight
      } yield result
  }

  /** The project's Scala compiler options, taken from the scala-maven-plugin `<args>` configuration, minus warnings
    * that would break mutation-switching code (notably fatal-warnings).
    */
  private def scalacOptions: Seq[String] =
    ScalaVersions.scalacOptions(project).filterNot(MavenCompiler.isBlocklistedScalacOption)

  /** All `.scala`/`.java` source files under the given roots, substituting the mutated copy from `tmpDir` when present
    * (sources not matched by the `files` config are not copied, so the original is used).
    */
  private def sourceFiles(roots: Seq[String], tmpDir: Path)(using Config): IO[Seq[Path]] =
    roots
      .flatTraverse: rootStr =>
        val root = Path(rootStr)
        Files[IO]
          .isDirectory(root)
          .ifM(
            Files[IO]
              .walk(root)
              .filter(MavenCompiler.isScalaOrJavaSource)
              .evalMap { source =>
                val mutated = source.inSubDir(tmpDir)
                Files[IO].exists(mutated).ifF(mutated, source)
              }
              .compile
              .toList,
            IO.pure(List.empty[Path])
          )

  private def cacheFile(tmpDir: Path, name: String): java.io.File =
    (tmpDir / s".zinc-$name.zip").toNioPath.toFile()

  /** Map a zinc compiler problem to a [[CompilerErrMsg]], relativized to `tmpDir` so [[stryker4s.run.RollbackHandler]]
    * can match it against the mutated file and roll back the offending mutant.
    */
  private def toCompilerErrMsg(problem: Problem, tmpDir: Path): Option[CompilerErrMsg] = {
    val position = problem.position()
    for {
      sourcePath <- position.sourcePath().toScala
      line <- position.line().toScala
    } yield {
      val relativePath = Path(sourcePath).relativePath(tmpDir).toString
      CompilerErrMsg(problem.message(), relativePath, line, position.offset().toScala.map(_.toInt))
    }
  }
}

object MavenCompiler {

  /** Compiled output directories of a [[MavenCompiler.compile]] run. */
  final case class Output(mainClasses: Path, testClasses: Path)

  private[maven] def originalOutputs(project: MavenProject): Set[String] =
    Set(project.getBuild().getOutputDirectory(), project.getBuild().getTestOutputDirectory())

  private[maven] def isOriginalOutput(project: MavenProject)(path: Path): Boolean =
    originalOutputs(project).contains(path.toString)

  private[maven] def resourceDirs(project: MavenProject): Seq[Path] = {
    val build = project.getBuild()
    (build.getResources().asScala ++ build.getTestResources().asScala).toSeq.map(r => Path(r.getDirectory()))
  }

  private val blocklistedScalacOptions: Set[String] =
    Set(
      "-Xfatal-warnings",
      "-Werror",
      "-Ycheck-all-patmat",
      "-Wdead-code",
      "-Ywarn-dead-code",
      "-Wvalue-discard",
      "-Ywarn-value-discard"
    )

  private val blocklistedScalacOptionPrefixes: Seq[String] = Seq("-Wunused", "-Ywarn-unused")

  /** Remove options that are very likely to cause errors with generated mutation-switching code
    * @see
    *   https://github.com/stryker-mutator/stryker4s/issues/321
    */
  private[runner] def isBlocklistedScalacOption(option: String): Boolean =
    blocklistedScalacOptions.contains(option) || blocklistedScalacOptionPrefixes.exists(option.startsWith)

  private[runner] def isScalaOrJavaSource(path: Path): Boolean =
    path.extName == ".scala" || path.extName == ".java"
}
