package stryker4s.report.dashboard
import stryker4s.report.model.DashboardConfig
import stryker4s.config.Config
import stryker4s.report.dashboard.Providers._
import stryker4s.env.Environment.Environment

class DashboardConfigProvider(env: Environment)(implicit config: Config) {
  def resolveConfig(): Either[String, DashboardConfig] =
    for {
      apiKey <- resolveapiKey()
      project <- resolveproject()
      version <- resolveversion()
      baseUrl = config.dashboard.baseUrl
      reportType = config.dashboard.reportType
      module = config.dashboard.module
    } yield DashboardConfig(
      apiKey = apiKey,
      project = project,
      version = version,
      baseUrl = baseUrl,
      reportType = reportType,
      module = module
    )

  private val apiKeyName = "STRYKER_DASHBOARD_API_KEY"
  private def resolveapiKey() =
    env
      .get(apiKeyName)
      .toRight(apiKeyName)

  private def resolveproject() =
    config.dashboard.project
      .orElse(byCiProvider(_.determineProject()))
      .toRight("dashboard.project")

  private def resolveversion() =
    config.dashboard.version
      .orElse(byCiProvider(_.determineVersion()))
      .toRight("dashboard.version")

  private def byCiProvider[T](f: CiProvider => Option[T]) = Providers.determineCiProvider(env).flatMap(f)
}
