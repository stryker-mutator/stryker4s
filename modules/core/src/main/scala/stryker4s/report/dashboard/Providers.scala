package stryker4s.report.dashboard

import cats.data.OptionT
import cats.effect.std.Env
import cats.syntax.functor.*
import cats.{Functor, Monad}

object Providers {
  def determineCiProvider[F[_]: Monad: Env](): OptionT[F, CiProvider[F]] =
    readEnvironmentVariable("TRAVIS")
      .as[CiProvider[F]](new TravisProvider[F]())
      .orElse(readEnvironmentVariable("CIRCLECI").as(new CircleProvider()))
      .orElse(readEnvironmentVariable("GITHUB_ACTION").as(new GithubActionsProvider()))

  sealed trait CiProvider[+F[_]] {
    def determineProject(): F[Option[String]]
    def determineVersion(): F[Option[String]]
  }

  private def readEnvironmentVariable[F[_]: Functor: Env](name: String): OptionT[F, String] =
    OptionT(Env[F].get(name)).filter(_.nonEmpty)

  /** TODO: Only github projects are supported for now
    */
  private val githubCom = "github.com"

  private class TravisProvider[F[_]: Functor: Env]() extends CiProvider[F] {
    override def determineProject(): F[Option[String]] =
      readEnvironmentVariable("TRAVIS_REPO_SLUG")
        .map(project => s"$githubCom/$project")
        .value

    override def determineVersion(): F[Option[String]] =
      readEnvironmentVariable("TRAVIS_BRANCH").value
  }

  private class CircleProvider[F[_]: Monad: Env]() extends CiProvider[F] {
    override def determineProject(): F[Option[String]] =
      (for {
        username <- readEnvironmentVariable("CIRCLE_PROJECT_USERNAME")
        repoName <- readEnvironmentVariable("CIRCLE_PROJECT_REPONAME")
      } yield s"$githubCom/$username/$repoName").value

    override def determineVersion(): F[Option[String]] =
      readEnvironmentVariable("CIRCLE_BRANCH").value
  }

  private class GithubActionsProvider[F[_]: Monad: Env]() extends CiProvider[F] {
    override def determineProject(): F[Option[String]] =
      readEnvironmentVariable("GITHUB_REPOSITORY")
        .map(project => s"$githubCom/$project")
        .value

    override def determineVersion(): F[Option[String]] =
      (for {
        ref <- readEnvironmentVariable("GITHUB_REF")
        refs = ref.split('/')
        version <- refs match {
          case Array(_, "pull", prNumber, _*) => OptionT.some(s"PR-$prNumber")
          case Array(_, _, tail*)             => OptionT.some(tail.mkString("/"))
          case _                              => OptionT.none[F, String]
        }
        if version.nonEmpty
      } yield version).value
  }

  // TODO: Support VSTS, GitLab CI
}
