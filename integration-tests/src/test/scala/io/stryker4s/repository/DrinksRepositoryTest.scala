package io.stryker4s.repository

import cats.effect.IO
import org.scalatest.{FunSpec, Matchers}

class DrinksRepositoryTest extends FunSpec with Matchers {

  describe("A Drink repository") {
    val sut: DrinksRepository[IO] = DrinksRepository.apply()

    it("returns all drinks when findAll is called") {
      val allDrinks = sut.findAll().unsafeRunSync()

      allDrinks.size shouldBe 4
    }

    it("returns the drink when it's known in the list") {
      sut.findByName("Robo water").unsafeRunSync() match {
        case Some(drink) =>
          drink.price shouldBe 0.50
          drink.name shouldBe "Robo water"
        case None =>
          fail("Drink was not found while it should")
      }
    }

    it("returns no drink when it's unknown") {
      sut.findByName("Unknown drink").unsafeRunSync() shouldBe None
    }

    it("returns all non alcoholic drinks when isAlcoholic is false") {
      val drinks = sut.findAllByIsAlcoholic(false).unsafeRunSync()

      // Intentional weak assertion the list has 4 drinks.
      // This will always return true because there are 2 alcoholic drinks and 2 non alcoholic drinks.
      drinks.size shouldBe 2
    }

    it("returns all alcoholic drinks when isAlcoholic is true") {
      val expectedDrinks = List("Robo Beer", "Rob(w)ine")

      val drinks = sut.findAllByIsAlcoholic(true).unsafeRunSync()

      drinks.size shouldBe 2
      drinks.map(_.name) should contain theSameElementsAs expectedDrinks
    }
  }
}
