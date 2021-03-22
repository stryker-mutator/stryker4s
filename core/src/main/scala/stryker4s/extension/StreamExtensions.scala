package stryker4s.extension

import fs2.Stream
import cats.effect.Concurrent
// import stryker4s.config.Config

object StreamExtensions {
  // implicit class RunConcurrentExtension[F[_], A](s: Stream[F, A]) {

  /** Runs the stream in n parallel executions
    */
  // def runConcurrent(n: Long)(implicit F: Concurrent[F]): Stream[F, A] =
  //   s.balanceAvailable // Fan out to 'infinite' concurrency
  //     .take(n) // Limit fan-out to 'n' streams
  //     .parJoinUnbounded // Join streams concurrently

  /** Run the stream in n parallel executions, using `concurrency` from config for the number of threads used
    */
  // def runConcurrent(implicit F: Concurrent[F], config: Config): Stream[F, A] =
  //   runConcurrent(config.concurrency.toLong)(F)
  // }

  implicit class ParEvalMapExtension[F[_]: Concurrent, O](mutants: fs2.Stream[F, O]) {
    def parEvalOn[O2, O3](
        testRunners: Stream[F, O2]
    )(testF: (O2, O) => F[O3]): Stream[F, O3] = {
      mutants.balanceAvailable
        .zip(testRunners)
        .map { case (bs, as) => bs.evalMap(testF(as, _)) }
        .parJoinUnbounded

    }
  }

}
