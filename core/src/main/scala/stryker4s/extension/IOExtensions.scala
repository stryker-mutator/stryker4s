package stryker4s.extension

import cats.effect.kernel.Sync
import cats.effect.syntax.all._
import cats.implicits._
import stryker4s.log.Logger

object IOExtensions {
  implicit class IOOps[F[_]: Sync, A](io: F[A]) {
    def logTimed(scope: String)(implicit logger: Logger): F[A] = io.timed.flatMap { case (duration, result) =>
      Sync[F].delay(logger.info(s"TIMED: $scope took ${duration.toMillis}ms")) *> result.pure[F]
    }
  }
}
