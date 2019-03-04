package io.stryker4s.endpoint

import cats.effect.Effect
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import io.stryker4s.entity.Order
import io.stryker4s.service.{OrderNotFound, OrderService, ValidationError}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}

import scala.language.higherKinds

object OrderEndpoint {
  def endpoints[F[_] : Effect](orderService: OrderService[F]): HttpRoutes[F] = {
    new OrderEndpoint[F].service(orderService)
  }
}

class OrderEndpoint[F[_] : Effect]() extends Http4sDsl[F] {

  implicit val orderDecoder: EntityDecoder[F, Order] = jsonOf[F, Order]

  def service(orderService: OrderService[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "order" / IntVar(id) =>
      orderService.find(id).value.flatMap {
        case Right(order) => Ok(order.asJson)
        case Left(OrderNotFound) => NotFound(s"Order $id could not be found.")
      }

    case req@POST -> Root / "order" =>
      val action = for {
        order <- req.as[Order]
        result <- orderService.placeOrder(order).value
      } yield result

      action.flatMap {
        case Right(placedOrder) =>
          Ok(placedOrder.asJson)
        case Left(validationError: ValidationError) =>
          BadRequest(validationError.asJson)
      }
  }
}
