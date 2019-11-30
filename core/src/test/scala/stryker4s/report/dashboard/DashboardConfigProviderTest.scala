package stryker4s.report.dashboard
import stryker4s.testutil.Stryker4sSuite
import stryker4s.config.Config
import stryker4s.report.model.DashboardConfig
import stryker4s.config.Full
import stryker4s.config.DashboardOptions
import stryker4s.config.MutationScoreOnly
import org.scalatest.EitherValues

class DashboardConfigProviderTest extends Stryker4sSuite with EitherValues {
  describe("resolveConfig") {
    it("should resolve a Travis environment") {
      implicit val config = Config.default
      val env = Map(
        "STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere",
        "TRAVIS" -> "true",
        "TRAVIS_REPO_SLUG" -> "travisRepoSlug",
        "TRAVIS_BRANCH" -> "travisBranch"
      )
      val sut = new DashboardConfigProvider(env)

      val result = sut.resolveConfig()

      result.right.value shouldBe DashboardConfig(
        "apiKeyHere",
        "https://dashboard.stryker-mutator.io",
        Full,
        "travisRepoSlug",
        "travisBranch",
        None
      )
    }

    it("should resolve a CircleCI environment") {
      implicit val config = Config.default
      val env = Map(
        "STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere",
        "CIRCLECI" -> "true",
        "CIRCLE_PROJECT_USERNAME" -> "circleUsername",
        "CIRCLE_PROJECT_REPONAME" -> "circleRepoName",
        "CIRCLE_BRANCH" -> "circleBranch"
      )
      val sut = new DashboardConfigProvider(env)

      val result = sut.resolveConfig()

      result.right.value shouldBe DashboardConfig(
        "apiKeyHere",
        config.dashboard.baseUrl,
        Full,
        "circleUsername/circleRepoName",
        "circleBranch",
        None
      )
    }

    it("should resolve a configured environment") {
      implicit val config = Config.default.copy(
        dashboard = DashboardOptions(
          baseUrl = "https://baseUrl.com",
          reportType = MutationScoreOnly,
          project = Some("projectHere"),
          version = Some("versionHere"),
          module = Some("moduleHere")
        )
      )
      val env = Map(
        "STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere"
      )
      val sut = new DashboardConfigProvider(env)

      val result = sut.resolveConfig()

      result.right.value shouldBe DashboardConfig(
        "apiKeyHere",
        "https://baseUrl.com",
        MutationScoreOnly,
        "projectHere",
        "versionHere",
        Some("moduleHere")
      )
    }

    it("should not resolve when there is no STRYKER_DASHBOARD_API_KEY environment") {
      implicit val config = Config.default
      val env = Map.empty[String, String]
      val sut = new DashboardConfigProvider(env)

      val result = sut.resolveConfig()

      result.left.value shouldBe "STRYKER_DASHBOARD_API_KEY"
    }

    it("should not resolve when there is no project") {
      implicit val config = Config.default.copy(
        dashboard = DashboardOptions(
          project = None,
          version = Some("versionHere"),
          module = Some("moduleHere")
        )
      )
      val env = Map("STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere")
      val sut = new DashboardConfigProvider(env)

      val result = sut.resolveConfig()

      result.left.value shouldBe "dashboard.project"
    }

    it("should not resolve when there is no version") {
      implicit val config = Config.default.copy(
        dashboard = DashboardOptions(
          project = Some("projectHere"),
          version = None,
          module = Some("moduleHere")
        )
      )
      val env = Map("STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere")
      val sut = new DashboardConfigProvider(env)

      val result = sut.resolveConfig()

      result.left.value shouldBe "dashboard.version"
    }

    it("should still resolve without a module") {
      implicit val config = Config.default.copy(
        dashboard = DashboardOptions(
          project = Some("projectHere"),
          version = Some("versionHere"),
          module = None
        )
      )
      val env = Map("STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere")
      val sut = new DashboardConfigProvider(env)

      val result = sut.resolveConfig()

      result.right.value shouldBe DashboardConfig(
        "apiKeyHere",
        config.dashboard.baseUrl,
        Full,
        "projectHere",
        "versionHere",
        None
      )
    }
  }
}
