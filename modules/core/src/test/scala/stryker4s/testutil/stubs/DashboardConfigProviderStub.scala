package stryker4s.testutil.stubs

import cats.data.NonEmptyChain
import cats.effect.IO
import cats.syntax.all.*
import stryker4s.report.dashboard.DashboardConfigProvider
import stryker4s.report.model.DashboardConfig

object DashboardConfigProviderStub {
  def apply(config: DashboardConfig): DashboardConfigProvider[IO] = () => config.validNec.pure[IO]

  def invalid(errors: NonEmptyChain[String]): DashboardConfigProvider[IO] = () => errors.invalid.pure[IO]
}
