package stryker4s.report.dashboard

import cats.data.ValidatedNec
import cats.syntax.apply.*
import cats.syntax.option.*
import stryker4s.config.Config
import stryker4s.env.Environment
import stryker4s.report.dashboard.Providers.*
import stryker4s.report.model.DashboardConfig

class DashboardConfigProvider(env: Environment)(implicit config: Config) {
  def resolveConfig(): ValidatedNec[String, DashboardConfig] =
    (resolveapiKey(), resolveproject(), resolveversion()).mapN {
      DashboardConfig(
        _,
        config.dashboard.baseUrl,
        config.dashboard.reportType,
        _,
        _,
        config.dashboard.module
      )
    }

  private val apiKeyName = "STRYKER_DASHBOARD_API_KEY"
  private def resolveapiKey() =
    env
      .get(apiKeyName)
      .toValidNec(apiKeyName)

  private def resolveproject() =
    config.dashboard.project
      .orElse(byCiProvider(_.determineProject()))
      .toValidNec("dashboard.project")

  private def resolveversion() =
    config.dashboard.version
      .orElse(byCiProvider(_.determineVersion()))
      .toValidNec("dashboard.version")

  private def byCiProvider[T](f: CiProvider => Option[T]) = Providers.determineCiProvider(env).flatMap(f)
}
