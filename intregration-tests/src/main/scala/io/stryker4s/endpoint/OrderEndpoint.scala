package io.stryker4s.endpoint

import cats.effect._
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

import scala.language.higherKinds

object OrderEndpoint {

  case class Order(id: Int, product: String)

  def service[F[_]](name: String)(implicit F: Effect[F]): HttpService[F] = HttpService[F] {
    case GET -> Root / "order" =>
      Response(Status.Ok).withBody(Json.obj("message" -> Json.fromString(s"Hello: $name")))

    case req @ POST -> Root / "order" =>
//      val order =      req.decodeJson[Order]

      Response(Status.Ok).withBody(Json.obj("message" -> Json.fromString(s"Hello: ${req.body}")))
  }
}
