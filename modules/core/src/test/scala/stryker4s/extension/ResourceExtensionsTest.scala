package stryker4s.extension

import cats.effect.{IO, Ref, Resource}
import stryker4s.extension.ResourceExtensions.SelfRecreatingResource
import stryker4s.testkit.Stryker4sIOSuite

class ResourceExtensionsTest extends Stryker4sIOSuite {
  describe("selfRecreatingResource") {
    test("should create a resource only once when not using the release F") {
      val op = for {
        log <- Ref[IO].of(List.empty[String])
        _ <- logged(log)
          .selfRecreatingResource { case (_, _) => IO.unit }
          .surround(log.get.assertEquals(List("open")))
        value <- log.get
      } yield value

      op.assertEquals(List("open", "close"))
    }

    test("should close first before creating a new Resource") {
      val op = for {
        log <- Ref[IO].of(List.empty[String])
        _ <- logged(log)
          .selfRecreatingResource { case (_, release) => release }
          .surround(log.get.assertEquals(List("open", "close", "open")))
        value <- log.get
      } yield value

      op.assertEquals(List("open", "close", "open", "close"))
    }

    def logged(log: Ref[IO, List[String]]): Resource[IO, Unit] =
      Resource.make(log.update(_ :+ "open"))(_ => log.update(_ :+ "close"))
  }
}
