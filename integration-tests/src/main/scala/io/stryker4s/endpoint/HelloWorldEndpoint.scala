package io.stryker4s.endpoint

import cats.effect.Effect
import io.circe.Json
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds

object HelloWorldEndpoint {
  def endpoints[F[_] : Effect](): HttpRoutes[F] = {
    new HelloWorldEndpoint[F].endpoints()
  }
}

class HelloWorldEndpoint[F[_] : Effect] extends Http4sDsl[F] {

  def endpoints(): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "hello" / name =>
      Ok(Json.obj("message" -> Json.fromString(s"Hello, $name")))
  }
}

