package stryker4s.extension

import stryker4s.testutil.Stryker4sSuite
import cats.effect.Clock
import cats.effect.IO
import stryker4s.extension.CatsOps._
import scala.concurrent.duration._

class CatsOpsTest extends Stryker4sSuite {

  describe("timed") {
    it("should give the time an execution takes") {
      implicit val clock: Clock[IO] = counterClock

      val (_, duration) = IO.unit.timed.unsafeRunSync()

      duration shouldBe 50.millis
    }

    it("should increase over longer execution times") {
      implicit val clock: Clock[IO] = counterClock

      val ((_, firstDuration), secondDuration) =
        // starttime 0
        IO.unit
          .flatMap(_ =>
            // starttime 50
            IO.unit.timed
          // endTime 100
          )
          .timed
          // endtime 150
          .unsafeRunSync()

      firstDuration shouldBe 50.millis
      secondDuration shouldBe 150.millis
    }
  }

  /** A 'clock' that counts the number of times it is invoked
    *
    * @return
    */
  def counterClock: Clock[IO] =
    new Clock[IO] {
      var time: Long = 0
      def realTime(unit: concurrent.duration.TimeUnit): IO[Long] = {
        time += 50
        IO.pure((FiniteDuration(time, unit)).length)
      }

      def monotonic(unit: concurrent.duration.TimeUnit): IO[Long] = ???
    }
}
