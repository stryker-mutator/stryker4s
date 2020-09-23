package stryker4s.extension

import cats.effect.{IO, Resource}
import stryker4s.extension.ResourceExtensions.SelfRecreatingResource
import stryker4s.testutil.Stryker4sSuite
import java.util.concurrent.atomic.AtomicInteger
import cats.effect.concurrent.Ref

class ResourceExtensionsTest extends Stryker4sSuite {
  describe("selfRecreatingResource") {
    it("should create itself only once") {
      val created = new AtomicInteger(0)
      val closed = new AtomicInteger(0)
      val resource =
        Resource.make(IO { created.incrementAndGet(); created })(_ => IO(closed.incrementAndGet()).void)

      val sut = resource.selfRecreatingResource { case (_: Ref[IO, AtomicInteger], _: IO[Unit]) => IO.pure(created) }
      sut.use(_ => IO(closed.get() shouldBe 0)).unsafeRunSync()

      created.get() shouldBe 1
      closed.get() shouldBe 1
    }

    it("should close when evaluating the close IO") {
      val created = new AtomicInteger(0)
      val closed = new AtomicInteger(0)
      val resource =
        Resource.make(IO { created.incrementAndGet(); created })(_ => IO(closed.incrementAndGet()).void)

      val sut = resource.selfRecreatingResource { case (_, release) => release *> IO.pure(created) }
      sut
        .use(_ =>
          IO {
            created.get() shouldBe 2
            closed.get() shouldBe 1
          }
        )
        .unsafeRunSync()

      created.get() shouldBe 2
      closed.get() shouldBe 2
    }
  }
}
