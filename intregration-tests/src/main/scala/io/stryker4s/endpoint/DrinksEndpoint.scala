package io.stryker4s.endpoint

import cats.effect.Effect
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import io.stryker4s.repository.DrinksRepository
import org.http4s.circe._
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._
import org.http4s.{HttpService, Response, Status}

import scala.language.higherKinds

object DrinksEndpoint {

  def service[F[_]](drinksRepository: DrinksRepository[F])(implicit F: Effect[F]): HttpService[F] = HttpService[F] {
    case GET -> Root / "drinks" =>
      drinksRepository
        .findAll()
        .flatMap(drinks => Response(Status.Ok).withBody(drinks.asJson))
  }

}
