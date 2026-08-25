package example

import cats.effect.IO
import cats.syntax.all.*

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

  // Exercises SequencingSwapMutator and SequencingRemovalMutator.
  def auditedOrderTotal: IO[Int] =
    IO.ref(0).flatMap { auditCount =>
      auditCount.update(_ + 1).as(1) *> auditCount.get.map(_ + 10)
    }

  // Exercises TimeoutRemovalMutator (removing .timeout lets the slow work finish, so the
  // quantity is returned instead of the -1 sentinel produced by the timeout recovery).
  def strictOrderTotal(quantity: Int): IO[Int] =
    IO.sleep(Deadlines.slowWork).as(quantity).timeout(Deadlines.limit).handleError(_ => -1)

  // Exercises TimeoutRemovalMutator (removing .timeoutTo discards the fallback effect, so the
  // quantity is returned instead of the -2 sentinel).
  def orderTotalWithDeadline(quantity: Int): IO[Int] =
    IO.sleep(Deadlines.slowWork).as(quantity).timeoutTo(Deadlines.limit, IO.pure(-2))
}
