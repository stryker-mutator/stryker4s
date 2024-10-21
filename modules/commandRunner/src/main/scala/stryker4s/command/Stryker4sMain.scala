package stryker4s.command

import cats.effect.{Deferred, ExitCode, IO, IOApp}
import stryker4s.run.threshold.ErrorStatus

import scala.concurrent.duration.FiniteDuration

object Stryker4sMain extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      log <- Stryker4sArgumentHandler.configureLogger(args)
      timeout <- Deferred[IO, FiniteDuration]
      result <- new Stryker4sCommandRunner(timeout, args)(log).run()
    } yield result match {
      case ErrorStatus => ExitCode.Error
      case _           => ExitCode.Success
    }
}
