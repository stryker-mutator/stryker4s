package stryker4jvm.run

import cats.data.NonEmptyList
import cats.effect.{IO, Ref, Resource}
import cats.syntax.traverse.*
import fs2.Stream

import scala.concurrent.duration.*

class ResourcePoolTest extends Stryker4jvmIOSuite {

  describe("resource") {
    it("should close the resources when closing the pool") {
      Ref[IO].of(false).flatMap { isClosed =>
        ResourcePool(NonEmptyList.one(testRunner("A").onFinalize(isClosed.set(true))))
          .use { pool =>
            // Before use
            isClosed.get.asserting(_ shouldBe false) *>
              // Using the testrunner resource
              Stream(1, 2, 3).covary[IO].through(pool.run { case (tr, mutant) => tr(mutant) }).compile.drain *>
              // After loan use
              isClosed.get.asserting(_ shouldBe false)
          } >> {
          // After pool `Resource` is closed
          isClosed.get.asserting(_ shouldBe true)
        }
      }
    }
  }

  describe("run") {
    it("should divide work on the resource pool") {
      val testRunners = NonEmptyList.of(testRunner("A"), testRunner("B"), testRunner("C"))

      val totalMutants = 10L
      val mutants = Stream.iterate[IO, Int](0)(_ + 1).take(totalMutants)
      ResourcePool(testRunners)
        .use { pool =>
          mutants.through(pool.run { case (tr, m) => tr(m) }).compile.toVector
        }
        .asserting { results =>
          results.size shouldBe totalMutants
          results.count(_._1 == "A") shouldBe >(1)
          results.count(_._1 == "B") shouldBe >(1)
          results.count(_._1 == "C") shouldBe >(1)
        }
    }

    it("should divide work over other resources if one is slower") {
      val testRunners = NonEmptyList.of(testRunner("A", 30.millis), testRunner("B"), testRunner("C"))

      val totalMutants = 10L
      val mutants = Stream.iterate[IO, Int](0)(_ + 1).take(totalMutants)
      ResourcePool(testRunners)
        .use { pool =>
          mutants.through(pool.run { case (tr, m) => tr(m) }).compile.toVector
        }
        .asserting { results =>
          results.size shouldBe totalMutants
          results.count(_._1 == "A") shouldBe >=(1)
          results.count(_._1 == "B") shouldBe >=(3)
          results.count(_._1 == "C") shouldBe >=(3)
        }
    }
  }

  describe("loan") {
    it("should put the resource back in the pool after use") {
      val testRunners = NonEmptyList.one(testRunner("A"))

      ResourcePool(testRunners)
        .use { pool =>
          // Two mutants on a single-testrunner pool
          List(0, 1).traverse { mutant =>
            pool.loan.use(_(mutant))
          }
        }
        .asserting { results =>
          results shouldBe List(("A", 0), ("A", 1))
        }
    }
  }

  def testRunner(name: String, duration: FiniteDuration = 2.millis) =
    Resource.pure[IO, Int => IO[(String, Int)]]((mutant: Int) => IO.sleep(duration).as((name, mutant)))
}
