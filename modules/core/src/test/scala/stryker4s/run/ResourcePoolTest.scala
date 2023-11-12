package stryker4s.run

import cats.data.NonEmptyList
import cats.effect.{IO, Ref, Resource}
import cats.syntax.traverse.*
import fs2.Stream
import stryker4s.testkit.Stryker4sIOSuite

import scala.concurrent.duration.*

class ResourcePoolTest extends Stryker4sIOSuite {

  describe("resource") {
    test("should close the resources when closing the pool") {
      Ref[IO].of(false).flatMap { isClosed =>
        ResourcePool(NonEmptyList.one(testRunner("A").onFinalize(isClosed.set(true))))
          .use { pool =>
            // Before use
            isClosed.get.assertEquals(false) *>
              // Using the testrunner resource
              Stream(1, 2, 3).covary[IO].through(pool.run { case (tr, mutant) => tr(mutant) }).compile.drain *>
              // After loan use
              isClosed.get.assertEquals(false)
          } >> {
          // After pool `Resource` is closed
          isClosed.get.assertEquals(true)
        }
      }
    }
  }

  describe("run") {
    test("should divide work on the resource pool") {
      val testRunners = NonEmptyList.of(testRunner("A"), testRunner("B"), testRunner("C"))

      val totalMutants = 10L
      val mutants = Stream.iterate[IO, Int](0)(_ + 1).take(totalMutants)
      ResourcePool(testRunners)
        .use { pool =>
          mutants.through(pool.run { case (tr, m) => tr(m) }).compile.toVector
        }
        .asserting { results =>
          assertEquals(results.size, totalMutants.toInt)
          assert(results.count(_._1 == "A") > 1)
          assert(results.count(_._1 == "B") > 1)
          assert(results.count(_._1 == "C") > 1)
        }
    }

    test("should divide work over other resources if one is slower") {
      val testRunners = NonEmptyList.of(testRunner("A", 30.millis), testRunner("B"), testRunner("C"))

      val totalMutants = 10L
      val mutants = Stream.iterate[IO, Int](0)(_ + 1).take(totalMutants)
      ResourcePool(testRunners)
        .use { pool =>
          mutants.through(pool.run { case (tr, m) => tr(m) }).compile.toVector
        }
        .asserting { results =>
          assertEquals(results.size, totalMutants.toInt)
          assert(results.count(_._1 == "A") >= 1)
          assert(results.count(_._1 == "B") >= 3)
          assert(results.count(_._1 == "C") >= 3)
        }
    }
  }

  describe("loan") {
    test("should put the resource back in the pool after use") {
      val testRunners = NonEmptyList.one(testRunner("A"))

      ResourcePool(testRunners)
        .use { pool =>
          // Two mutants on a single-testrunner pool
          List(0, 1).traverse { mutant =>
            pool.loan.use(_(mutant))
          }
        }
        .asserting { results =>
          assertEquals(results, List(("A", 0), ("A", 1)))
        }
    }
  }

  def testRunner(name: String, duration: FiniteDuration = 2.millis) =
    Resource.pure[IO, Int => IO[(String, Int)]]((mutant: Int) => IO.sleep(duration).as((name, mutant)))
}
