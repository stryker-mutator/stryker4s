package stryker4s.scalatest

import org.scalactic.Equality
import stryker4s.extension.TreeExtensions.IsEqualExtension

import scala.meta.Tree

/** Provides equality checking for ScalaTest on the structure of Trees.
  * Checks if two trees have the same structure and syntax
  * Can be used as follows: <code>firstTree should equal(secondTree)</code>.
  * If this trait is in scope but you still want to check for reference equality,
  * the <code>be</code> matcher can be used instead of <code>equal</code>
  */
trait TreeEquality {
  // Needs to be T <: Tree to work with subtypes of Tree instead of just Tree
  implicit def structureEquality[T <: Tree]: Equality[T] =
    (first: T, secondAny: Any) =>
      secondAny match {
        case second: Tree => second.isEqual(first)
        case _            => false
    }
}
