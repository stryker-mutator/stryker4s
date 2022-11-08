package stryker4jvm.extensions

import cats.{Monoid, Semigroup}
import cats.syntax.semigroup.*

object PartialFunctionOps {

  /** `Monoid` to combine two `PartialFunctions`s into one.
    *
    * Instead of stopping after the first PartialFunction matches, combining will try to match both PartialFunctions,
    * and combine the result
    */
  implicit def partialFunctionMonoid[A, B: Semigroup]: Monoid[PartialFunction[A, B]] =
    new Monoid[PartialFunction[A, B]] {

      def combine(x: PartialFunction[A, B], y: PartialFunction[A, B]): PartialFunction[A, B] =
        Function.unlift(x.lift.combine(y.lift))

      def empty: PartialFunction[A, B] = PartialFunction.empty

    }

}
