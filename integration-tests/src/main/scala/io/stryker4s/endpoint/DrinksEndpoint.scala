package io.stryker4s.endpoint

import cats.effect.Effect
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import io.stryker4s.service.{DrinkNotFound, DrinksService}
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds

object DrinksEndpoint {

  def endpoints[F[_]: Effect](drinksService: DrinksService[F]): HttpRoutes[F] = {
    new DrinksEndpoint[F].endpoints(drinksService)
  }
}

class DrinksEndpoint[F[_]: Effect] extends Http4sDsl[F] {

  object IsAlcoholicQueryParamMatcher extends QueryParamDecoderMatcher[Boolean]("isAlcoholic")

  def endpoints(drinksService: DrinksService[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "drinks" :? IsAlcoholicQueryParamMatcher(isAlcoholic) =>
      drinksService
        .findByIsAlcoholic(isAlcoholic)
        .flatMap(drinks => Ok(drinks.asJson))

    case GET -> Root / "drinks" =>
      drinksService
        .findAllDrinks()
        .flatMap(drinks => Ok(drinks.asJson))

    case GET -> Root / "drinks" / name =>
      drinksService
        .findByName(name)
        .value
        .flatMap {
          case Right(drink)        => Ok(drink.asJson)
          case Left(DrinkNotFound) => NotFound("The Robobar doesn't service this drink.")
        }
  }
}
