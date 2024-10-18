package stryker4s.sbt

import cats.effect.unsafe.IORuntime
import cats.effect.{Deferred, IO}
import sbt.*
import sbt.Keys.*
import sbt.plugins.*
import stryker4s.config.DashboardReportType
import stryker4s.config.source.CliConfigSource
import stryker4s.log.{Logger, SbtLogger}
import stryker4s.run.threshold.ErrorStatus
import stryker4s.sbt.*
import sttp.model.Uri

import scala.concurrent.duration.FiniteDuration
import scala.meta.Dialect

/** This plugin adds a new task (stryker) to the project that allows you to run mutation testing over your code
  */
object Stryker4sPlugin extends AutoPlugin {
  override def requires = JvmPlugin

  override def trigger = allRequirements

  object autoImport {
    // Task
    val stryker = inputKey[Unit]("Run Stryker4s mutation testing")

    // Settings
    val strykerMinimumSbtVersion = settingKey[String]("Lowest supported sbt version by Stryker4s")
    val strykerIsSupported = settingKey[Boolean]("If running Stryker4s is supported on this sbt version")

    // Config settings
    val strykerMutate = settingKey[Seq[String]]("Pattern(s) of files to mutate")
    val strykerBaseDir = settingKey[File]("The base directory of the project")
    val strykerTestFilter = settingKey[Seq[String]]("Filter out tests to run")
    val strykerReporters = settingKey[Seq[String]]("Reporter(s) to use")
    val strykerFiles = settingKey[Seq[String]]("Pattern(s) of files to include in mutation testing")
    val strykerExcludedMutations = settingKey[Seq[String]]("Mutations to exclude from mutation testing")
    val strykerThresholdsHigh = settingKey[Int]("Threshold for high mutation score")
    val strykerThresholdsLow = settingKey[Int]("Threshold for low mutation score")
    val strykerThresholdsBreak = settingKey[Int]("Threshold score for breaking the build")
    val strykerDashboardBaseUrl = settingKey[String]("Base-url for the dashboard reporter")
    val strykerDashboardReportType =
      settingKey[DashboardReportType]("Type of dashboard report (full, mutationScoreOnly)")
    val strykerDashboardProject =
      settingKey[Option[String]]("Dashboard project identifier. Format of `gitProvider/organization/repository`")
    val strykerDashboardVersion = settingKey[Option[String]]("Dashboard version identifier")
    val strykerDashboardModule = settingKey[String]("Dashboard module identifier (optional)")
    val strykerTimeout = settingKey[FiniteDuration](
      "Timeout for a single mutation test run. timeoutForTestRun = netTime * timeoutFactor + timeout"
    )
    val strykerTimeoutFactor = settingKey[Double](
      "Timeout factor single mutation test run. timeoutForTestRun = netTime * timeoutFactor + timeout"
    )
    val strykerMaxTestRunnerReuse = settingKey[Int]("Restart the testrunner after every `n` runs.")
    val strykerLegacyTestRunner = settingKey[Boolean]("Use the legacy test runner (not recommended)")
    val strykerScalaDialect = settingKey[Dialect]("Dialect for parsing Scala files")
    val strykerConcurrency = settingKey[Int]("Number of testrunners to start")
    val strykerDebugLogTestRunnerStdout = settingKey[Boolean]("Log test-runner output to debug log")
    val strykerDebugDebugTestRunner = settingKey[Boolean]("Pass JVM debugging parameters to the test-runner")
    val strykerStaticTmpDir = settingKey[Boolean]("Force temporary dir to a static path (`target/stryker4s-tmpDir`)")
    val strykerCleanTmpDir = settingKey[Boolean]("Remove the tmpDir after a successful mutation test run")
  }

  import autoImport.*

  // Default settings for the plugin
  override lazy val projectSettings: Seq[Def.Setting[?]] = Seq(
    stryker := strykerTask.evaluated,
    stryker / logLevel := Level.Info,
    stryker / onLoadMessage := "", // Prevents "[info] Set current project to ..." in between mutations
    strykerMinimumSbtVersion := "1.7.0",
    strykerIsSupported := SemanticSelector(s">=${strykerMinimumSbtVersion.value}")
      .matches(VersionNumber(sbtVersion.value)),

    // Config settings
    strykerBaseDir := getStryker4sBaseDir.value,
    strykerMutate := getSourceDirectories("**.scala").value,
    strykerFiles := getSourceDirectories("**").value,
    strykerDashboardProject := scmInfo.value.flatMap(toDashboardProject),
    strykerDashboardVersion := gitCurrentBranch.?.value.filterNot(_.isBlank()),
    strykerDashboardModule := normalizedName.value
  )

  /** Dynamically load git-current-branch from sbt-git if it is installed, for the dashboard version config
    */
  private val gitCurrentBranch = SettingKey[String]("git-current-branch")

  /** sbt-crossproject base-directory setting
    */
  private val crossProjectBaseDirectory = SettingKey[File]("crossProjectBaseDirectory")

  /** sbt-projectmatrix base-directory setting
    */
  private val projectMatrixBaseDirectory = SettingKey[File]("projectMatrixBaseDirectory")

  private def getSourceDirectories(postfix: String) = Def.setting {
    (Compile / unmanagedSourceDirectories).value
      .flatMap(_.relativeTo(strykerBaseDir.value))
      .map(file => (file / postfix).toString)
  }

  private def getStryker4sBaseDir = Def.setting[File] {
    crossProjectBaseDirectory.?.value
      .orElse(projectMatrixBaseDirectory.?.value)
      .orElse(sourceDirectory.?.value.flatMap(f => if (f.getName() == "src") Option(f.getParentFile) else none))
      .getOrElse(baseDirectory.value)
  }

  /** Parse scmInfo to a dashboard project identifier, using the format of `gitProvider/organization/repository`.
    *
    * E.g. `github.com/stryker-mutator/stryker4s`
    */
  private def toDashboardProject(scmInfo: ScmInfo): Option[String] = {
    val uri = Uri(scmInfo.browseUrl.toURI())
    for {
      base <- uri.host
      if base.startsWith("github.com")
      path = uri.path.mkString("/")
      if !path.isBlank()
    } yield s"$base/$path"
  }

  lazy val strykerTask = Def.inputTaskDyn {
    // Call logLevel so it shows up as a used setting when set
    val _ = (stryker / logLevel).value
    val sbtLog = streams.value.log

    if (!strykerIsSupported.value) {
      throw new UnsupportedSbtVersionException(
        s"Sbt version ${sbtVersion.value} is not supported by Stryker4s. Please upgrade to a later version. The lowest supported version is ${strykerMinimumSbtVersion.value}. If you know what you are doing you can override this with the 'strykerIsSupported' sbt setting."
      )
    }

    val sbtConfig = SbtConfigSource().value

    Def.task {
      implicit val runtime: IORuntime = IORuntime.global
      implicit val logger: Logger = new SbtLogger(sbtLog)
      val parsed = sbt.complete.DefaultParsers.spaceDelimited("<arg>").parsed

      val extraConfigSources = List(sbtConfig, new CliConfigSource(parsed))

      Deferred[IO, FiniteDuration] // Create shared timeout between testrunners
        .map(new Stryker4sSbtRunner(state.value, _, extraConfigSources))
        .flatMap(_.run())
        .flatMap {
          case ErrorStatus => IO.raiseError(new MessageOnlyException("Mutation score is below configured threshold"))
          case _           => IO.unit
        }
        .unsafeRunSync()
    }
  }

  private class UnsupportedSbtVersionException(s: String)
      extends IllegalArgumentException(s)
      with FeedbackProvidedException
}
