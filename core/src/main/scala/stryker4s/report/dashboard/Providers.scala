package stryker4s.report.dashboard

import stryker4s.env.Environment

object Providers {
  def determineCiProvider(env: Environment): Option[CiProvider] =
    if (readEnvironmentVariable("TRAVIS", env).isDefined) {
      Some(new TravisProvider(env))
    } else if (readEnvironmentVariable("CIRCLECI", env).isDefined) {
      Some(new CircleProvider(env))
    } else if (readEnvironmentVariable("GITHUB_ACTION", env).isDefined) {
      Some(new GithubActionsProvider(env))
    } else {
      None
    }

  sealed trait CiProvider {
    def determineProject(): Option[String]
    def determineVersion(): Option[String]
  }

  private def readEnvironmentVariable(name: String, env: Environment): Option[String] =
    env.get(name).filter(_.nonEmpty)

  /** TODO: Only github projects are supported for now
    */
  private val githubCom = "github.com"

  class TravisProvider(env: Environment) extends CiProvider {
    override def determineProject(): Option[String] =
      readEnvironmentVariable("TRAVIS_REPO_SLUG", env)
        .map(project => s"$githubCom/$project")

    override def determineVersion(): Option[String] =
      readEnvironmentVariable("TRAVIS_BRANCH", env)
  }

  class CircleProvider(env: Environment) extends CiProvider {
    override def determineProject(): Option[String] =
      for {
        username <- readEnvironmentVariable("CIRCLE_PROJECT_USERNAME", env)
        repoName <- readEnvironmentVariable("CIRCLE_PROJECT_REPONAME", env)
      } yield s"$githubCom/$username/$repoName"

    override def determineVersion(): Option[String] =
      readEnvironmentVariable("CIRCLE_BRANCH", env)
  }

  class GithubActionsProvider(env: Environment) extends CiProvider {
    override def determineProject(): Option[String] =
      readEnvironmentVariable("GITHUB_REPOSITORY", env)
        .map(project => s"$githubCom/$project")

    override def determineVersion(): Option[String] =
      for {
        ref <- readEnvironmentVariable("GITHUB_REF", env)
        refs = ref.split('/')
        version <- refs match {
          case Array(_, "pull", prNumber, _*) => Some(s"PR-$prNumber")
          case Array(_, _, tail*)             => Some(tail.mkString("/"))
          case _                              => None
        }
        if version.nonEmpty
      } yield version
  }

  // TODO: Support VSTS, GitLab CI
}
