package stryker4s.report.mapper

import cats.syntax.option.*
import fs2.io.file.Path
import mutationtesting.*
import stryker4s.config.{Config, Thresholds as ConfigThresholds}
import stryker4s.model.*

import java.nio.file.Files
import scala.util.Try

trait MutantRunResultMapper {
  protected[report] def toReport(
      results: Map[Path, Seq[MutantRunResult]]
  )(implicit config: Config): MutationTestResult[Config] =
    MutationTestResult(
      thresholds = toThresholds(config.thresholds),
      files = toFileResultMap(results),
      projectRoot = config.baseDir.absolute.toString.some,
      config = config.some,
      system = systemInformation.some,
      framework = frameworkInformation.some
    )

  private def toThresholds(thresholds: ConfigThresholds): Thresholds =
    Thresholds(high = thresholds.high, low = thresholds.low)

  private def toFileResultMap(
      results: Map[Path, Seq[MutantRunResult]]
  )(implicit config: Config): Map[String, FileResult] =
    results.map { case (path, runResults) =>
      path.toString.replace('\\', '/') -> toFileResult(path, runResults)
    }

  private def toFileResult(path: Path, runResults: Seq[MutantRunResult])(implicit
      config: Config
  ): FileResult =
    FileResult(
      fileContentAsString(path),
      runResults.map(toMutantResult)
    )

  private def toMutantResult(runResult: MutantRunResult): MutantResult = {
    val mutant = runResult.mutant
    MutantResult(
      mutant.id.globalId.toString,
      mutant.mutationType.mutationName,
      mutant.mutated.syntax,
      toLocation(mutant.original.pos),
      toMutantStatus(runResult),
      runResult.description,
      testsCompleted = runResult.testsCompleted
    )
  }

  private def toLocation(pos: scala.meta.inputs.Position): Location =
    Location(
      start = Position(line = pos.startLine + 1, column = pos.startColumn + 1),
      end = Position(line = pos.endLine + 1, column = pos.endColumn + 1)
    )

  private def toMutantStatus(mutant: MutantRunResult): MutantStatus =
    mutant match {
      case _: Survived     => MutantStatus.Survived
      case _: Killed       => MutantStatus.Killed
      case _: NoCoverage   => MutantStatus.NoCoverage
      case _: TimedOut     => MutantStatus.Timeout
      case _: Error        => MutantStatus.RuntimeError
      case _: Ignored      => MutantStatus.Ignored
      case _: CompileError => MutantStatus.CompileError
    }

  private def fileContentAsString(path: Path)(implicit config: Config): String =
    new String(Files.readAllBytes((config.baseDir / path).toNioPath))

  private def systemInformation: SystemInformation = SystemInformation(
    ci = sys.env.contains("CI"),
    os = OSInformation(platform = sys.props("os.name"), version = sys.props("os.version").some).some,
    cpu = CpuInformation(logicalCores = Runtime.getRuntime().availableProcessors()).some,
    ram = RamInformation(total = Runtime.getRuntime().totalMemory()).some
  )

  private def frameworkInformation: FrameworkInformation = {
    val stryker4sVersion = Try(this.getClass().getPackage().getImplementationVersion()).toOption
      .flatMap(Option(_)) // null if not packaged

    FrameworkInformation(
      name = "Stryker4s",
      version = stryker4sVersion,
      branding = brandingInformation.some
    )
  }

  private def brandingInformation: BrandingInformation =
    BrandingInformation(
      homepageUrl = "https://stryker-mutator.io",
      imageUrl =
        "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill-rule='evenodd' stroke-linejoin='round' stroke-miterlimit='2' clip-rule='evenodd' viewBox='0 0 1458 1458'%3E%3Cpath fill='none' d='M0 0h1458v1458H0z'/%3E%3CclipPath id='a'%3E%3Cpath d='M0 0h1458v1458H0z'/%3E%3C/clipPath%3E%3Cg clip-path='url(%23a)'%3E%3Cpath fill='%23e74c3c' fill-rule='nonzero' d='M1458 729c0 402.7-326.3 729-729 729S0 1131.7 0 729a729 729 0 0 1 1458 0'/%3E%3Cpath fill-opacity='.3' d='m778.3 1456.2-201.7-201.8 233-105 85-78.7v-64.3l-257-257-44-187-50-208 251.8-82.8 281.2 117.8 380.1 379.2A729 729 0 0 1 778.3 1456z'/%3E%3Cpath fill='%23f1c40f' fill-rule='nonzero' d='M753.4 329.5c41.8 0 74.6 7.8 98 25.4 23.5 18 41.6 44 55 77.1l11.8 28.7 165.7-58.2-14.2-32a343.2 343.2 0 0 0-114.1-144.1C906.2 191 838 172.1 750.7 172.1c-50.8 0-95.6 7.4-134.8 21.5-40 14.7-74 34.8-102.2 60.3a257.7 257.7 0 0 0-65.5 92.7A287.4 287.4 0 0 0 426.1 459c0 72.5 20.7 133.3 61.2 182.7 38.6 47.3 98.3 88 179.8 121.3 42.3 17.5 78.7 33.1 109.3 47a247 247 0 0 1 66.1 41.7 129.5 129.5 0 0 1 33.6 49.3c7.8 20.2 11.2 45.7 11.2 76.4 0 28-4.3 51.8-13.6 71.2a119.9 119.9 0 0 1-34.5 44.2 139.4 139.4 0 0 1-49.4 24.5 222 222 0 0 1-58.7 8c-29.4 0-54.4-3.4-75.2-10.8-20-7-37.1-16-51.2-27.4a147 147 0 0 1-33.8-38.3 253 253 0 0 1-23-48.4l-11-31.4-161.7 60.6 10.8 30.1a370.5 370.5 0 0 0 42 82.8 303 303 0 0 0 69.6 72.7 342 342 0 0 0 99.4 51c37.8 12.7 82 19.2 132.6 19.2 50 0 95.8-8.3 137.6-24.6 42.2-16.5 78.4-39 108.8-67.3a307 307 0 0 0 71.9-100.7 296.5 296.5 0 0 0 25.9-122.2c0-54.3-8.4-100.4-24.2-138.3a298 298 0 0 0-66-98.8 385.3 385.3 0 0 0-93.8-67.2 1108.6 1108.6 0 0 0-106.6-47.5 745.9 745.9 0 0 1-90-39.6 239 239 0 0 1-53.5-37.3 97.4 97.4 0 0 1-24.7-37.6c-5.4-15.5-8-33.4-8-53.6 0-40.9 11.3-71.5 37-90.5 28.5-20.9 65-30.7 109.4-30.7z'/%3E%3Cpath d='M720 0h18v113h-18zm738 738v-18h-113v18h113zm-738 607h18v113h-18zM113 738v-18H0v18h113z'/%3E%3C/g%3E%3C/svg%3E".some
    )
}
