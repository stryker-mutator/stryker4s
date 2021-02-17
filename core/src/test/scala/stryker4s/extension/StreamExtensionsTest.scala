package stryker4s.extension

import stryker4s.testutil.Stryker4sIOSuite
import fs2.Stream
import cats.effect.IO

import StreamExtensions._
import scala.concurrent.ExecutionContext
class StreamExtensionsTest extends Stryker4sIOSuite {
  implicit override def executionContext: ExecutionContext = ExecutionContext.global

  describe("parEvalOn") {
    //
    it("should run the stream on multiple given items from the stream") {
      def tr(prefix: Int) = (a: Int) => IO(s"$prefix;$a")

      val size = 10L
      val mutants = Stream.iterate(0)(_ + 1).take(size)
      val trs = Stream.iterate(0)(_ + 1).map(tr).take(3)

      val result = mutants.covary[IO].parEvalOn(trs)({ case (tr, a) => tr(a) })

      result.compile.toVector.asserting(results => {
        val (zeros, r) = results.partition(_.startsWith("0"))
        val (ones, twos) = r.partition(_.startsWith("1"))

        zeros.length + ones.length + twos.length shouldBe size
        zeros.length shouldBe >(1)
        ones.length shouldBe >(1)
        twos.length shouldBe >(1)
      })
    }
  }
}
