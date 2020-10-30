package stryker4s.extension

import cats.effect.{IO, Resource}
import stryker4s.extension.ResourceExtensions.SelfRecreatingResource
import stryker4s.testutil.Stryker4sIOSuite
import cats.effect.concurrent.Ref

class ResourceExtensionsTest extends Stryker4sIOSuite {
  describe("selfRecreatingResource") {
    it("should create a resource only once when not using the release F") {
      val op = for {
        log <- Ref[IO].of(List.empty[String])
        _ <- logged(log)
          .selfRecreatingResource { case (_, _) => IO.unit }
          .use(_ => log.get.asserting(_ should contain.only("open")))
        value <- log.get
      } yield value

      op.asserting(_ shouldBe List("open", "close"))
    }

    it("should close first before creating a new Resource") {
      val op = for {
        log <- Ref[IO].of(List.empty[String])
        _ <- logged(log)
          .selfRecreatingResource { case (_, release) => release }
          .use(_ => log.get.map(_ shouldBe List("open", "close", "open")))
        value <- log.get
      } yield value

      op.asserting(_ shouldBe List("open", "close", "open", "close"))
    }

    def logged(log: Ref[IO, List[String]]): Resource[IO, Unit] =
      Resource.make(log.update(_ :+ "open"))(_ => log.update(_ :+ "close"))
  }
}
