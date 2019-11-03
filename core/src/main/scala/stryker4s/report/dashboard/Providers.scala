package stryker4s.report.dashboard
import grizzled.slf4j.Logging

object Providers extends Logging {
  trait CiProvider {
    def isPullRequest: Boolean
    def determineProject(): Option[String]
    def determineVersion(): Option[String]
    def determineApiKey(): Option[String] = readEnvironmentVariableOrLog("STRYKER_DASHBOARD_API_KEY")

    protected def readEnvironmentVariableOrLog(name: String): Option[String] = {
      val environmentOption = sys.env.get(name).filter(_.nonEmpty)
      if (environmentOption.isEmpty) {
        warn(
          s"Missing environment variable $name, not initializing ${this.getClass.getSimpleName} for dashboard reporter."
        )
      }
      environmentOption
    }
  }

  object TravisProvider extends CiProvider {
    override def isPullRequest: Boolean = !readEnvironmentVariableOrLog("TRAVIS_PULL_REQUEST").forall(_ == "false")
    override def determineProject(): Option[String] = readEnvironmentVariableOrLog("TRAVIS_REPO_SLUG")
    override def determineVersion(): Option[String] = readEnvironmentVariableOrLog("TRAVIS_BRANCH")
  }

  object CircleProvider extends CiProvider {
    override def isPullRequest: Boolean = !readEnvironmentVariableOrLog("CIRCLE_PULL_REQUEST").forall(_ == "false")
    override def determineProject(): Option[String] =
      for {
        username <- readEnvironmentVariableOrLog("CIRCLE_PROJECT_USERNAME")
        repoName <- readEnvironmentVariableOrLog("CIRCLE_PROJECT_REPONAME")
      } yield s"$username/$repoName"
    override def determineVersion(): Option[String] = readEnvironmentVariableOrLog("CIRCLE_BRANCH")
  }
}
