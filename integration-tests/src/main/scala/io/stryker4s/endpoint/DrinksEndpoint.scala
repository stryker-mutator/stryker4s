package io.stryker4s.endpoint

import cats.effect.Effect
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import io.stryker4s.repository.DrinksRepository
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds

object DrinksEndpoint {
  def endpoints[F[_] : Effect](drinksRepository: DrinksRepository[F]): HttpRoutes[F] = {
    new DrinksEndpoint[F].endpoints(drinksRepository)
  }
}

class DrinksEndpoint[F[_] : Effect] extends Http4sDsl[F] {

  def endpoints(drinksRepository: DrinksRepository[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "drinks" =>
      drinksRepository
        .findAll()
        .flatMap(drinks => Ok(drinks.asJson))
  }
}
