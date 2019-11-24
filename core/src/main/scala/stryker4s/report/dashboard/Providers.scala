package stryker4s.report.dashboard
import grizzled.slf4j.Logging
import stryker4s.env.Environment

object Providers extends Logging {
  def determineCiProvider(env: Environment): Option[CiProvider] =
    if (env.getEnvVariable("TRAVIS").isDefined) {
      Some(new TravisProvider(env))
    } else if (env.getEnvVariable("CIRCLECI").isDefined) {
      Some(new CircleProvider(env))
    } else {
      None
    }

  trait CiProvider {
    def determineProject(): Option[String]
    def determineVersion(): Option[String]
  }

  private def readEnvironmentVariable(name: String, env: Environment): Option[String] =
    env.getEnvVariable(name).filter(_.nonEmpty)

  class TravisProvider(env: Environment) extends CiProvider {
    override def determineProject(): Option[String] =
      readEnvironmentVariable("TRAVIS_REPO_SLUG", env)

    override def determineVersion(): Option[String] =
      readEnvironmentVariable("TRAVIS_BRANCH", env)
  }

  class CircleProvider(env: Environment) extends CiProvider {
    override def determineProject(): Option[String] =
      for {
        username <- readEnvironmentVariable("CIRCLE_PROJECT_USERNAME", env)
        repoName <- readEnvironmentVariable("CIRCLE_PROJECT_REPONAME", env)
      } yield s"$username/$repoName"

    override def determineVersion(): Option[String] =
      readEnvironmentVariable("CIRCLE_BRANCH", env)
  }
}
