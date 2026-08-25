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
}
