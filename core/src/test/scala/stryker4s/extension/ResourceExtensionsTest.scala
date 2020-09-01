package stryker4s.extension

import stryker4s.testutil.Stryker4sSuite
import cats.effect.Resource
import cats.effect.IO
import stryker4s.extension.ResourceExtensions._

class ResourceExtensionsTest extends Stryker4sSuite {
  describe("selfRecreatingResource") {
    it("should create itself only once") {
      var created = 0
      var closed = 0
      val resource = Resource.make(IO(created += 1))(_ => IO(closed += 1))

      val sut = resource.selfRecreatingResource { case _ => IO.unit }
      sut.use(_ => IO(closed shouldBe 0)).unsafeRunSync()

      created shouldBe 1
      closed shouldBe 1
    }

    it("should close when evaluating the close IO") {
      var timesCreated = 0
      var timesClosed = 0
      val resource = Resource.make(IO(timesCreated += 1))(_ => IO(timesClosed += 1))

      val sut = resource.selfRecreatingResource { case (_, release) => release *> IO.unit }
      sut
        .use(_ =>
          IO {
            timesCreated shouldBe 2
            timesClosed shouldBe 1
          }
        )
        .unsafeRunSync()

      timesCreated shouldBe 2
      timesClosed shouldBe 2
    }
  }
}
