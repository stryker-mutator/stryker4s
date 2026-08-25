package example

import cats.effect.IO

object Calculator {
  def add(a: Int, b: Int): Int = a + b
  def subtract(a: Int, b: Int): Int = a - b

  // Exercises NumericLiteralMutator (10 is a numeric literal threshold).
  def isLargeOrder(quantity: Int): Boolean = quantity > 10

  // Exercises CollectionLiteralMutator (a non-empty List literal).
  def defaultDiscounts: List[Int] = List(5, 10, 15)

  // Exercises ErrorHandlingMutator (dropping .handleErrorWith removes the division-by-zero
  // recovery, causing the ArithmeticException to propagate instead of being recovered to 0).
  def safeDivide(a: Int, b: Int): IO[Int] =
    IO(a / b).handleErrorWith(_ => IO.pure(0))

  // Exercises ConditionalEffectMutator (flipping IO.whenA to IO.unlessA inverts the guard).
  def rejectOversizedOrder(quantity: Int): IO[Unit] =
    IO.whenA(quantity > 100)(IO.raiseError(new IllegalArgumentException("oversized order")))

  // Exercises ConditionalEffectMutator (flipping IO.raiseUnless to IO.raiseWhen inverts the guard).
  def requirePositive(value: Int): IO[Int] =
    IO.raiseUnless(value > 0)(new IllegalArgumentException("non-positive")).as(value)

  // Exercises FallbackRemovalMutator and FallbackInversionMutator.
  def orderTotalWithFallback(primaryAvailable: Boolean): IO[Int] =
    (if (primaryAvailable) IO.pure(7)
     else IO.raiseError(new IllegalStateException("primary unavailable")))
      .orElse(IO.pure(42))
}
