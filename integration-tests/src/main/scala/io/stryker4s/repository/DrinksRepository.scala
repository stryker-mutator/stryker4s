package io.stryker4s.repository

import cats._
import cats.implicits._
import io.stryker4s.entity.Drink

import scala.language.higherKinds

object DrinksRepository {

  private[this] val drinks = List(
    Drink("Robo Cola", 1.25, isAlcoholic = false),
    Drink("Robo Beer", 2.00, isAlcoholic = true),
    Drink("Rob(w)ine", 3.00, isAlcoholic = true),
    Drink("Robo water", 0.50, isAlcoholic = false)
  )

  def apply[F[_]: Applicative]() = new DrinksRepository[F](drinks)
}

class DrinksRepository[F[_]: Applicative](private[this] val drinks: List[Drink]) {

  def findAll(): F[List[Drink]] = {
    drinks.pure[F]
  }

  def findByName(name: String): F[Option[Drink]] = {
    drinks.find(_.name == name).pure[F]
  }

  def findAllByIsAlcoholic(isAlcoholic: Boolean): F[List[Drink]] = {
    drinks.filter(_.isAlcoholic == isAlcoholic).pure[F]
  }

}
