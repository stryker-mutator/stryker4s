package stryker4s.report.dashboard
import grizzled.slf4j.Logging

object Providers extends Logging {

  trait CiProvider {
    def isPullRequest: Boolean
    def determineBranch(): Option[String]
    def determineRepository(): Option[String]
    def determineApiKey(): Option[String] = readEnvironmentVariableOrLog("STRYKER_DASHBOARD_API_KEY")

    protected def readEnvironmentVariableOrLog(name: String): Option[String] = {
      val environmentOption = sys.env.get(name).filter(_.nonEmpty)
      if (environmentOption.isEmpty) {
        warn(
          s"Missing environment variable $name, not initializing ${this.getClass.getSimpleName} for dashboard reporter.")
      }
      environmentOption
    }
  }

  object TravisProvider extends CiProvider {
    override def isPullRequest: Boolean = !readEnvironmentVariableOrLog("TRAVIS_PULL_REQUEST").forall(_ == "false")
    override def determineBranch(): Option[String] = readEnvironmentVariableOrLog("TRAVIS_BRANCH")
    override def determineRepository(): Option[String] = readEnvironmentVariableOrLog("TRAVIS_REPO_SLUG")
  }

  object CircleProvider extends CiProvider {
    override def isPullRequest: Boolean = !readEnvironmentVariableOrLog("CIRCLE_PULL_REQUEST").forall(_ == "false")
    override def determineBranch(): Option[String] = readEnvironmentVariableOrLog("CIRCLE_BRANCH")
    override def determineRepository(): Option[String] =
      for {
        username <- readEnvironmentVariableOrLog("CIRCLE_PROJECT_USERNAME")
        repoName <- readEnvironmentVariableOrLog("CIRCLE_PROJECT_REPONAME")
      } yield s"$username/$repoName"
  }
}
