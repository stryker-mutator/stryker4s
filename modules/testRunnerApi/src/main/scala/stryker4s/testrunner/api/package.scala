package stryker4s.testrunner

import scalapb.TypeMapper

import scala.concurrent.duration.{Duration, FiniteDuration}

package object api {

  /** @see
    *   https://scalapb.github.io/docs/customizations#custom-types
    */
  implicit val finiteDurationTypeMapper: TypeMapper[Long, FiniteDuration] =
    TypeMapper[Long, FiniteDuration](Duration.fromNanos)(_.toNanos)
}
