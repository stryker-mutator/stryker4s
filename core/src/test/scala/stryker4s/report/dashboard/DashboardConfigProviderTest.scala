package stryker4s.report.dashboard

import cats.Id
import cats.data.NonEmptyChain
import cats.effect.std.Env
import cats.syntax.validated.*
import org.scalatest.EitherValues
import stryker4s.config.{Config, DashboardOptions, Full, MutationScoreOnly}
import stryker4s.report.model.DashboardConfig
import stryker4s.testutil.Stryker4sSuite
import sttp.client3.UriContext

class DashboardConfigProviderTest extends Stryker4sSuite with EitherValues {

  /** Create a cats-effect Env using 'Id' (not suspended in anything) as the effect type
    */
  def makeEnv(entr: (String, String)*): Env[Id] = new Env[Id] {
    val env = entr.toMap

    override def entries = env
    override def get(name: String): Option[String] = env.get(name)
  }

  describe("resolveConfig") {
    it("should resolve a Travis environment") {
      implicit val config = Config.default
      implicit val env: Env[Id] = makeEnv(
        "STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere",
        "TRAVIS" -> "true",
        "TRAVIS_REPO_SLUG" -> "travisRepo/slug",
        "TRAVIS_BRANCH" -> "travisBranch"
      )
      val sut = new DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      result shouldBe DashboardConfig(
        "apiKeyHere",
        uri"https://dashboard.stryker-mutator.io",
        Full,
        "github.com/travisRepo/slug",
        "travisBranch",
        None
      ).valid

    }

    it("should resolve a CircleCI environment") {
      implicit val config = Config.default
      implicit val env = makeEnv(
        "STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere",
        "CIRCLECI" -> "true",
        "CIRCLE_PROJECT_USERNAME" -> "circleUsername",
        "CIRCLE_PROJECT_REPONAME" -> "circleRepoName",
        "CIRCLE_BRANCH" -> "circleBranch"
      )
      val sut = new DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      result shouldBe DashboardConfig(
        "apiKeyHere",
        config.dashboard.baseUrl,
        Full,
        "github.com/circleUsername/circleRepoName",
        "circleBranch",
        None
      ).valid

    }

    it("should resolve a GitHub actions environment") {
      implicit val config = Config.default
      implicit val env = makeEnv(
        "STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere",
        "GITHUB_ACTION" -> "true",
        "GITHUB_REPOSITORY" -> "github/repo",
        "GITHUB_REF" -> "refs/heads/feat/branch-1"
      )
      val sut = new DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      result shouldBe DashboardConfig(
        "apiKeyHere",
        config.dashboard.baseUrl,
        Full,
        "github.com/github/repo",
        "feat/branch-1",
        None
      ).valid
    }

    it("should resolve a GitHub actions PR environment") {
      implicit val config = Config.default
      implicit val env = makeEnv(
        "STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere",
        "GITHUB_ACTION" -> "true",
        "GITHUB_REPOSITORY" -> "github/repo",
        "GITHUB_REF" -> "refs/pull/10/merge"
      )
      val sut = new DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      result shouldBe DashboardConfig(
        "apiKeyHere",
        config.dashboard.baseUrl,
        Full,
        "github.com/github/repo",
        "PR-10",
        None
      ).valid
    }

    it("should resolve a configured environment") {
      implicit val config = Config.default.copy(
        dashboard = DashboardOptions(
          baseUrl = uri"https://baseUrl.com",
          reportType = MutationScoreOnly,
          project = Some("projectHere"),
          version = Some("versionHere"),
          module = Some("moduleHere")
        )
      )
      implicit val env = makeEnv(
        "STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere"
      )
      val sut = new DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      result shouldBe DashboardConfig(
        "apiKeyHere",
        uri"https://baseUrl.com",
        MutationScoreOnly,
        "projectHere",
        "versionHere",
        Some("moduleHere")
      ).valid
    }

    it("should resolve without a module") {
      implicit val config = Config.default.copy(
        dashboard = DashboardOptions(
          project = Some("projectHere"),
          version = Some("versionHere"),
          module = None
        )
      )
      implicit val env = makeEnv("STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere")
      val sut = new DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      result shouldBe DashboardConfig(
        "apiKeyHere",
        config.dashboard.baseUrl,
        Full,
        "projectHere",
        "versionHere",
        None
      ).valid
    }

    it("should not resolve a GitHub actions with malformed ref") {
      implicit val config = Config.default
      implicit val env = makeEnv(
        "STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere",
        "GITHUB_ACTION" -> "true",
        "GITHUB_REPOSITORY" -> "github/repo",
        "GITHUB_REF" -> "refs/whatever"
      )
      val sut = new DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      result shouldBe ("dashboard.version".invalidNec)
    }

    it("should not resolve empty env variables") {
      implicit val config = Config.default
      implicit val env = makeEnv(
        "STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere",
        "CIRCLECI" -> "true",
        "CIRCLE_PROJECT_USERNAME" -> "circleUsername",
        "CIRCLE_PROJECT_REPONAME" -> "circleRepoName",
        "CIRCLE_BRANCH" -> ""
      )
      val sut = new DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      result shouldBe ("dashboard.version".invalidNec)
    }

    it("should not resolve when there is no STRYKER_DASHBOARD_API_KEY environment") {
      implicit val config = Config.default.copy(
        dashboard = DashboardOptions(
          project = Some("projectHere"),
          version = Some("versionHere"),
          module = Some("moduleHere")
        )
      )
      implicit val env = makeEnv()
      val sut = new DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      result shouldBe ("STRYKER_DASHBOARD_API_KEY".invalidNec)
    }

    it("should not resolve when there is no project") {
      implicit val config = Config.default.copy(
        dashboard = DashboardOptions(
          project = None,
          version = Some("versionHere"),
          module = Some("moduleHere")
        )
      )
      implicit val env = makeEnv("STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere")
      val sut = new DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      result shouldBe ("dashboard.project".invalidNec)
    }

    it("should not resolve when there is no version") {
      implicit val config = Config.default.copy(
        dashboard = DashboardOptions(
          project = Some("projectHere"),
          version = None,
          module = Some("moduleHere")
        )
      )
      implicit val env = makeEnv("STRYKER_DASHBOARD_API_KEY" -> "apiKeyHere")
      val sut = new DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      result shouldBe ("dashboard.version".invalidNec)
    }

    it("should return all unresolved") {
      implicit val config = Config.default.copy(
        dashboard = DashboardOptions(
          project = None,
          version = None,
          module = Some("moduleHere")
        )
      )
      implicit val env = makeEnv()
      val sut = new DashboardConfigProvider[Id]()

      val result = sut.resolveConfig()

      result shouldBe (NonEmptyChain("STRYKER_DASHBOARD_API_KEY", "dashboard.project", "dashboard.version").invalid)
    }
  }
}
