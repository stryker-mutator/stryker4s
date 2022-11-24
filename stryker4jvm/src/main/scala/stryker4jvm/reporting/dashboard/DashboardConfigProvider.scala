package stryker4jvm.reporting.dashboard

import cats.data.ValidatedNec
import cats.syntax.apply.*
import cats.syntax.option.*
import stryker4jvm.config.Config
import stryker4jvm.env.Environment
import stryker4jvm.reporting.dashboard.Providers.*
import stryker4jvm.reporting.model.DashboardConfig

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
