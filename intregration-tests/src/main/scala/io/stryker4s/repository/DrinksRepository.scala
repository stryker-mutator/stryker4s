package io.stryker4s.repository

import cats.effect.{Effect, IO}
import io.stryker4s.entity.Drink

import scala.language.higherKinds

class DrinksRepository[F[_]](private[this] val drinks: List[Drink])(implicit e: Effect[F]) {

  def findAll(): F[List[Drink]] = {
    e pure drinks
  }

  def findByName(name: String): F[Option[Drink]] = {
    e pure drinks.find(_.name == name)
  }

}

object DrinksRepository {

  private[this] val drinks = List(
    Drink("Robo Cola", 1.25, isAlcoholic = false),
    Drink("Robo Beer", 2.00, isAlcoholic = true),
    Drink("Rob(w)ine", 3.00, isAlcoholic = true),
    Drink("Robo water", 0.50, isAlcoholic = false)
  )

  def empty[F[_]](implicit m: Effect[F]): IO[DrinksRepository[F]] = IO {
    new DrinksRepository[F](drinks)
  }
}
