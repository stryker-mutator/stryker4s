package io.stryker4s.repository

import cats.Applicative
import cats.implicits._
import io.stryker4s.entity.Order

import scala.collection.concurrent.TrieMap
import scala.language.higherKinds

object OrderRepository {
  def apply[F[_]: Applicative](): OrderRepository[F] = new OrderRepository[F]()
}

class OrderRepository[F[_]: Applicative] {

  private val orders = new TrieMap[Int, Order]

  def create(order: Order): F[Order] = {
    orders += (order.id -> order)
    order.pure[F]
  }

  def find(orderId: Int): F[Option[Order]] = {
    orders
      .collectFirst {
        case (id, order) if id == orderId => order
      }
      .pure[F]
  }
}
