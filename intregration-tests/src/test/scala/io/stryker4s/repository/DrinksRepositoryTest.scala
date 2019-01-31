package io.stryker4s.repository

import cats.effect.IO
import org.scalatest.{FunSpec, Matchers}

class DrinksRepositoryTest extends FunSpec with Matchers {

  describe("A Drink repository") {

    it("returns all drinks when findAll is called") {
      val sut = createSut()

      val allDrinks = sut.findAll().unsafeRunSync()

      allDrinks.size shouldBe 4
    }

  }

  private[this] def createSut(): DrinksRepository[IO] = {
    DrinksRepository.empty[IO].unsafeRunSync()
  }

}
