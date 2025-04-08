package stryker4s.report.dashboard

import cats.Id
import cats.data.NonEmptyChain
import cats.effect.std.Env
import cats.syntax.all.*
import stryker4s.config.{Config, Full, MutationScoreOnly}
import stryker4s.report.model.DashboardConfig
import stryker4s.testkit.Stryker4sSuite
import stryker4s.testutil.stubs.EnvStub
import sttp.client4.UriContext

class DashboardConfigProviderTest extends Stryker4sSuite {

  describe("resolveConfig") {
    test("should resolve a Travis environment") {
      implicit val config: Config = Config.default
      implicit val env: Env[Id] = EnvStub.makeEnv[Id](
        ("STRYKER_DASHBOARD_API_KEY", "apiKeyHere"),
        ("TRAVIS", "true"),
        ("TRAVIS_REPO_SLUG", "travisRepo/slug"),
        ("TRAVIS_BRANCH", "travisBranch")
      )
      val sut = DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      assertEquals(
        result,
        DashboardConfig(
          "apiKeyHere",
          uri"https://dashboard.stryker-mutator.io",
          Full,
          "github.com/travisRepo/slug",
          "travisBranch",
          none
        ).valid
      )

    }

    test("should resolve a CircleCI environment") {
      implicit val config: Config = Config.default
      implicit val env: Env[Id] = EnvStub.makeEnv[Id](
        ("STRYKER_DASHBOARD_API_KEY", "apiKeyHere"),
        ("CIRCLECI", "true"),
        ("CIRCLE_PROJECT_USERNAME", "circleUsername"),
        ("CIRCLE_PROJECT_REPONAME", "circleRepoName"),
        ("CIRCLE_BRANCH", "circleBranch")
      )
      val sut = DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      assertEquals(
        result,
        DashboardConfig(
          "apiKeyHere",
          config.dashboard.baseUrl,
          Full,
          "github.com/circleUsername/circleRepoName",
          "circleBranch",
          none
        ).valid
      )

    }

    test("should resolve a GitHub actions environment") {
      implicit val config: Config = Config.default
      implicit val env: Env[Id] = EnvStub.makeEnv[Id](
        "STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere",
        "GITHUB_ACTION" -> "true",
        "GITHUB_REPOSITORY" -> "github/repo",
        "GITHUB_REF" -> "refs/heads/feat/branch-1"
      )
      val sut = DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      assertEquals(
        result,
        DashboardConfig(
          "apiKeyHere",
          config.dashboard.baseUrl,
          Full,
          "github.com/github/repo",
          "feat/branch-1",
          none
        ).valid
      )
    }

    test("should resolve a GitHub actions PR environment") {
      implicit val config: Config = Config.default
      implicit val env: Env[Id] = EnvStub.makeEnv[Id](
        "STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere",
        "GITHUB_ACTION" -> "true",
        "GITHUB_REPOSITORY" -> "github/repo",
        "GITHUB_REF" -> "refs/pull/10/merge"
      )
      val sut = DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      assertEquals(
        result,
        DashboardConfig(
          "apiKeyHere",
          config.dashboard.baseUrl,
          Full,
          "github.com/github/repo",
          "PR-10",
          none
        ).valid
      )
    }

    test("should resolve a configured environment") {
      implicit val config: Config = Config.default.copy(
        dashboard = Config.default.dashboard.copy(
          baseUrl = uri"https://baseUrl.com",
          reportType = MutationScoreOnly,
          project = "projectHere".some,
          version = "versionHere".some,
          module = "moduleHere".some
        )
      )
      implicit val env: Env[Id] = EnvStub.makeEnv[Id](
        "STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere"
      )
      val sut = DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      assertEquals(
        result,
        DashboardConfig(
          "apiKeyHere",
          uri"https://baseUrl.com",
          MutationScoreOnly,
          "projectHere",
          "versionHere",
          "moduleHere".some
        ).valid
      )
    }

    test("should resolve without a module") {
      implicit val config: Config = Config.default.copy(
        dashboard = Config.default.dashboard.copy(
          project = "projectHere".some,
          version = "versionHere".some,
          module = none
        )
      )
      implicit val env: Env[Id] = EnvStub.makeEnv[Id](("STRYKER_DASHBOARD_API_KEY", "apiKeyHere"))
      val sut = DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      assertEquals(
        result,
        DashboardConfig(
          "apiKeyHere",
          config.dashboard.baseUrl,
          Full,
          "projectHere",
          "versionHere",
          none
        ).valid
      )
    }

    test("should not resolve a GitHub actions with malformed ref") {
      implicit val config: Config = Config.default
      implicit val env: Env[Id] = EnvStub.makeEnv[Id](
        ("STRYKER_DASHBOARD_API_KEY", "apiKeyHere"),
        ("GITHUB_ACTION", "true"),
        ("GITHUB_REPOSITORY", "github/repo"),
        ("GITHUB_REF", "refs/whatever")
      )
      val sut = DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      assertEquals(result, "dashboard.version".invalidNec)
    }

    test("should not resolve empty env variables") {
      implicit val config: Config = Config.default
      implicit val env: Env[Id] = EnvStub.makeEnv[Id](
        ("STRYKER_DASHBOARD_API_KEY", "apiKeyHere"),
        ("CIRCLECI", "true"),
        ("CIRCLE_PROJECT_USERNAME", "circleUsername"),
        ("CIRCLE_PROJECT_REPONAME", "circleRepoName"),
        ("CIRCLE_BRANCH", "")
      )
      val sut = DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      assertEquals(result, "dashboard.version".invalidNec)
    }

    test("should not resolve when there is no STRYKER_DASHBOARD_API_KEY environment") {
      implicit val config: Config = Config.default.copy(
        dashboard = Config.default.dashboard.copy(
          project = "projectHere".some,
          version = "versionHere".some,
          module = "moduleHere".some
        )
      )
      implicit val env: Env[Id] = EnvStub.makeEnv[Id]()
      val sut = DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      assertEquals(result, "STRYKER_DASHBOARD_API_KEY".invalidNec)
    }

    test("should not resolve when there is no project") {
      implicit val config: Config = Config.default.copy(
        dashboard = Config.default.dashboard.copy(
          project = none,
          version = "versionHere".some,
          module = "moduleHere".some
        )
      )
      implicit val env: Env[Id] = EnvStub.makeEnv[Id](("STRYKER_DASHBOARD_API_KEY", "apiKeyHere"))
      val sut = DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      assertEquals(result, "dashboard.project".invalidNec)
    }

    test("should not resolve when there is no version") {
      implicit val config: Config = Config.default.copy(
        dashboard = Config.default.dashboard.copy(
          project = "projectHere".some,
          version = none,
          module = "moduleHere".some
        )
      )
      implicit val env: Env[Id] = EnvStub.makeEnv[Id](("STRYKER_DASHBOARD_API_KEY", "apiKeyHere"))
      val sut = DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      assertEquals(result, "dashboard.version".invalidNec)
    }

    test("should return all unresolved") {
      implicit val config: Config = Config.default.copy(
        dashboard = Config.default.dashboard.copy(
          project = none,
          version = none,
          module = "moduleHere".some
        )
      )
      implicit val env: Env[Id] = EnvStub.makeEnv[Id]()
      val sut = DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      assertEquals(result, NonEmptyChain("STRYKER_DASHBOARD_API_KEY", "dashboard.project", "dashboard.version").invalid)
    }
  }
}
