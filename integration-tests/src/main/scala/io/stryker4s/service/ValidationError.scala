package io.stryker4s.service

sealed trait ValidationError extends Product with Serializable

case object DrinkNotFound extends ValidationError
