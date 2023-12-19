package stryker4s.testkit

import cats.effect.IO
import munit.internal.difflib.Diff
import munit.{CatsEffectSuite, Compare, FunSuite, Location}

import scala.meta.*

sealed trait Stryker4sAssertions {
  this: FunSuite =>

  def describe(description: String)(body: => Unit): Unit = {
    val start = munitTestsBuffer.size
    body
    val end = munitTestsBuffer.size
    (start until end).foreach { i =>
      munitTestsBuffer(i) = {
        val t = munitTestsBuffer(i)
        t.withName(s"$description > ${t.name}")
      }
    }
  }

  def assertSameElements[A](
      obtained: Iterable[A],
      expected: Iterable[A]
  )(implicit loc: Location): Unit = {
    lazy val d = new Diff(obtained.toString, expected.toString)
    obtained.foreach(a =>
      assert(
        expected.exists(_ == a),
        d.createReport(s"Expected $a to be in $expected")
      )
    )
    assertEquals(
      obtained.size,
      expected.size,
      d.createReport(s"Expected same size, but got ${obtained.size} and ${expected.size}")
    )
  }

  def assertMatchPattern[A](obtained: A, matchFn: PartialFunction[Any, Unit])(implicit loc: Location): Unit =
    assert(matchFn.isDefinedAt(obtained), s"Expected $obtained to match pattern")

  def assertNotMatchPattern[A <: Any](obtained: A, matchFn: PartialFunction[Any, Unit])(implicit loc: Location): Unit =
    assert(!matchFn.isDefinedAt(obtained), s"Expected $obtained to not match pattern")

  implicit class EitherValues[A, B](either: Either[A, B]) {

    /** Like `.get`, but with a nicer error message.
      */
    def value(implicit loc: Location): B = either.getOrElse(fail(s"Expected Right, but got $either"))

    /** Like `.left.get`, but with a nicer error message.
      */
    def leftValue(implicit loc: Location): A = either.left.getOrElse(fail(s"Expected Left, but got $either"))
  }

  implicit class OptionValues[A](option: Option[A]) {

    /** Like `.get`, but with a nicer error message.
      */
    def value(implicit loc: Location): A = option.getOrElse(fail(s"Expected Some, but got $option"))
  }

  implicit class IterableLoneElement[A](it: Iterable[A]) {
    def loneElement(implicit loc: Location): A =
      it.size match {
        case 0 => fail("Expected a single element, but got an empty collection")
        case 1 => it.head
        case other => fail(s"""Expected a single element, but got size $other
                              |Elements: $it""".stripMargin)
      }
  }

  implicit class IOSeqAssertions[A](io: IO[Seq[A]]) {
    def assertSameElementsAs(expected: Seq[A])(implicit loc: Location): IO[Unit] =
      io.flatMap(a => IO(assertSameElements(a, expected)))
  }

  implicit class IOAssertions[A](io: IO[A]) {
    def asserting(f: A => Unit): IO[Unit] =
      io.flatMap(a => IO(f(a)))
  }

  /** Compare 2 trees by structure.
    */
  implicit def treeCompare[A <: Tree, B <: Tree]: Compare[A, B] = (obtained: A, expected: B) =>
    obtained == expected || obtained.structure == expected.structure

}

abstract protected[stryker4s] class Stryker4sSuite extends FunSuite with Stryker4sAssertions

abstract protected[stryker4s] class Stryker4sIOSuite extends CatsEffectSuite with Stryker4sAssertions
