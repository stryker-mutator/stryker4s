package io.stryker4s.service

sealed trait ValidationError extends Product with Serializable

case object DrinkNotFound extends ValidationError

case class OrderNotAllowed(reason: String) extends ValidationError

case object OrderNotFound extends ValidationError