package io.stryker4s.endpoint

import cats.effect._
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import scala.language.higherKinds

object OrderEndpoint {
  def endpoints[F[_]: Effect](name: String): HttpRoutes[F] = {
    new OrderEndpoint[F].service(name)
  }
}

class OrderEndpoint[F[_]: Effect] extends Http4sDsl[F]{

  case class Order(id: Int, product: String)

  def service(name: String): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "order" =>
      Ok(Json.obj("message" -> Json.fromString(s"Hello: $name")))

    case req @ POST -> Root / "order" =>
//      val order =      req.decodeJson[Order]

      Ok(Json.obj("message" -> Json.fromString(s"Hello: ${req.body}")))
  }
}
