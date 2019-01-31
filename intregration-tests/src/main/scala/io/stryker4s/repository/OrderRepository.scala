package io.stryker4s.repository

import cats.effect.Effect
import io.stryker4s.entity.Order

import scala.language.higherKinds

class OrderRepository[F[_]](implicit e: Effect[F]) {

//  def placeOrder(order: Order)
}
