package io.stryker4s.service

import cats.Monad
import cats.data.EitherT
import io.stryker4s.entity.Drink
import io.stryker4s.repository.DrinksRepository

import scala.language.higherKinds

object DrinksService {
  def apply[F[_]](drinksRepository: DrinksRepository[F]): DrinksService[F] = new DrinksService(drinksRepository)
}

class DrinksService[F[_]](drinksRepository: DrinksRepository[F]) {

  def findAllDrinks(): F[List[Drink]] = {
    drinksRepository.findAll()
  }

  def findByIsAlcoholic(isAlcoholic: Boolean): F[List[Drink]] = {
    drinksRepository.findByIsAlcoholic(isAlcoholic)
  }

  def findByName(name: String)(implicit M: Monad[F]): EitherT[F, DrinkNotFound.type, Drink] = {
    EitherT.fromOptionF(drinksRepository.findByName(name), DrinkNotFound)
  }

}
