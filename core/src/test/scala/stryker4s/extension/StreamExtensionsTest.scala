package stryker4s.extension

import scala.concurrent.ExecutionContext

import cats.effect.IO
import fs2.Stream
import stryker4s.extension.StreamExtensions._
import stryker4s.testutil.Stryker4sIOSuite
import scala.concurrent.duration._
import cats.effect.Resource

class StreamExtensionsTest extends Stryker4sIOSuite {
  override def executionContext: ExecutionContext = ExecutionContext.global

  describe("parEvalOn") {
    it("should run the stream on multiple given items from the stream") {
      val size = 10L
      val mutants = Stream.iterate(0)(_ + 1).take(size)
      val trs = Stream.iterate(0)(_ + 1).map(tr).take(3)

      val result = mutants.covary[IO].parEvalOn(trs) { case (tr, a) => tr(a) }

      result.compile.toVector.asserting { results =>
        val (zeros, r) = results.partition(_.startsWith("0"))
        val (ones, twos) = r.partition(_.startsWith("1"))

        zeros.length + ones.length + twos.length shouldBe size
        zeros.length shouldBe >(1)
        ones.length shouldBe >(1)
        twos.length shouldBe >(1)
      }
    }

    it("should only start as many testrunners as there are mutants") {
      val size = 1L
      val mutants = Stream.iterate(0)(_ + 1).take(size)
      val tr0 = Stream.resource(Resource.make(IO(tr(0)))(_ => IO.unit))
      // val tr1 =
      //   Stream.resource(
      //     Resource.make(IO.raiseError[Int => IO[String]](new Exception(s"should not make testrunner ${1}")))(_ =>
      //       IO.unit
      //     )
      //   )
      // val tr2 =
      //   Stream.resource(
      //     Resource.make(IO.raiseError[Int => IO[String]](new Exception(s"should not make testrunner ${2}")))(_ =>
      //       IO.unit
      //     )
      //   )
      val trs = tr0 ++ tr0

      val result = mutants.covary[IO].parEvalOn(trs) { case (tr, a) => tr(a) }

      result.compile.toVector.asserting { results =>
        val (zeros, r) = results.partition(_.startsWith("0"))
        val (ones, twos) = r.partition(_.startsWith("1"))

        zeros.length shouldBe 1
        ones.length shouldBe 0
        twos.length shouldBe 0
      }
    }

    it("should eval equally if one item is slower") {
      val size = 10L
      val mutants = Stream.iterate(0)(_ + 1).take(size)
      val slowTR = (a: Int) => IO.sleep(50.millis) *> IO(s"0;$a")
      val tr1 = tr(1)
      val tr2 = tr(2)
      val trs = Stream(slowTR, tr1, tr2)
      val result = mutants.covary[IO].parEvalOn(trs) { case (tr, a) => tr(a) }

      result.compile.toVector.asserting { results =>
        val (zeros, r) = results.partition(_.startsWith("0"))
        val (ones, twos) = r.partition(_.startsWith("1"))

        zeros.length + ones.length + twos.length shouldBe size
        zeros.length shouldBe 1
        ones.length shouldBe >(1)
        twos.length shouldBe >(1)
      }
    }

    it("should eval equally on resource streams") {
      val size = 10L
      val mutants = Stream.iterate(0)(_ + 1).take(size)
      val tr0 = Stream.resource(Resource.make(IO(tr(0)))(_ => IO.unit))
      val tr1 = Stream.resource(Resource.make(IO(tr(1)))(_ => IO.unit))
      val tr2 = Stream.resource(Resource.make(IO(tr(2)))(_ => IO.unit))
      val trs = tr0 ++ tr1 ++ tr2
      val result = mutants.covary[IO].parEvalOn(trs) { case (tr, a) => tr(a) }

      result.compile.toVector.asserting { results =>
        val (zeros, r) = results.partition(_.startsWith("0"))
        val (ones, twos) = r.partition(_.startsWith("1"))

        zeros.length + ones.length + twos.length shouldBe size
        zeros.length shouldBe >(1)
        ones.length shouldBe >(1)
        twos.length shouldBe >(1)
      }

    }
  }

  def tr(prefix: Int) = (a: Int) => IO(s"$prefix;$a")

  // class SignaledResource[T](canStop: Signal[IO, Unit], canStart: Signal[IO, Unit], resource: Resource[IO, T])
}
