package stryker4jvm.extensions

import cats.effect.{IO, Ref, Resource}
import stryker4jvm.extensions.ResourceExtensions.SelfRecreatingResource
import stryker4jvm.testutil.Stryker4jvmIOSuite

class ResourceExtensionsTest extends Stryker4jvmIOSuite {
  describe("selfRecreatingResource") {
    it("should create a resource only once when not using the release F") {
      val op = for {
        log <- Ref[IO].of(List.empty[String])
        _ <- logged(log)
          .selfRecreatingResource { case (_, _) => IO.unit }
          .surround(log.get.asserting(_ should contain.only("open")))
        value <- log.get
      } yield value

      op.asserting(_ shouldBe List("open", "close"))
    }

    it("should close first before creating a new Resource") {
      val op = for {
        log <- Ref[IO].of(List.empty[String])
        _ <- logged(log)
          .selfRecreatingResource { case (_, release) => release }
          .surround(log.get.map(_ shouldBe List("open", "close", "open")))
        value <- log.get
      } yield value

      op.asserting(_ shouldBe List("open", "close", "open", "close"))
    }

    def logged(log: Ref[IO, List[String]]): Resource[IO, Unit] =
      Resource.make(log.update(_ :+ "open"))(_ => log.update(_ :+ "close"))
  }
}
