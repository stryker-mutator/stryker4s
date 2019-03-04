package io.stryker4s.service

import cats.Monad
import cats.data.EitherT
import io.stryker4s.entity.Order
import io.stryker4s.repository.OrderRepository
import io.stryker4s.validator.OrderValidator

import scala.language.higherKinds

object OrderService {

  def apply[F[_]](orderRepository: OrderRepository[F], orderValidator: OrderValidator[F]): OrderService[F] =
    new OrderService(orderRepository, orderValidator)
}

class OrderService[F[_]](orderRepository: OrderRepository[F], orderValidator: OrderValidator[F]) {

  def placeOrder(order: Order)(implicit M: Monad[F]): EitherT[F, OrderNotAllowed, Order] = {
    for {
      _ <- EitherT.fromEither(orderValidator.allowedToOrderDrinks(order.age, order.drinks))
      saved <- EitherT.liftF(orderRepository.create(order))
    } yield saved
  }

  def find(id: Int)(implicit M: Monad[F]): EitherT[F, OrderNotFound.type, Order] = {
    EitherT.fromOptionF(orderRepository.find(id), OrderNotFound)
  }
}
