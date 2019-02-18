package io.stryker4s.validator

import cats._
import io.stryker4s.entity.Drink
import io.stryker4s.service.OrderNotAllowed

import scala.language.higherKinds

object OrderValidator {
  def apply[F[_] : Monad](): OrderValidator[F] = new OrderValidator[F]()
}

class OrderValidator[F[_] : Monad] {

  def allowedToOrderDrinks(age: Int, drinks: List[Drink]): Either[OrderNotAllowed, Unit] = {
    if (!canDrink(age)) {
      if (drinks.exists(_.isAlcoholic)) {
        Left(OrderNotAllowed(s"Customer is not allowed to order alcoholic drinks at $age years old"))
      } else {
        Right(())
      }
    } else {
      Right(())
    }
  }

  private[this] def canDrink(age: Int): Boolean = {
    age >= 18
  }
}
