package io.stryker4s.repository

import cats.effect.IO
import org.scalatest.{FunSpec, Matchers}

class DrinksRepositoryTest extends FunSpec with Matchers {

  describe("A Drink repository") {

    it("returns all drinks when findAll is called") {
      val sut: DrinksRepository[IO] = DrinksRepository.apply()

      val allDrinks = sut.findAll().unsafeRunSync()

      allDrinks.size shouldBe 4
    }
  }
}
