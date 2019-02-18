package io.stryker4s.repository

import cats.Applicative

import scala.language.higherKinds

object OrderRepository {
  def apply[F[_]: Applicative](): OrderRepository[F] = new OrderRepository[F]()
}

class OrderRepository[F[_]] {

//  def placeOrder(order: Order)
}
