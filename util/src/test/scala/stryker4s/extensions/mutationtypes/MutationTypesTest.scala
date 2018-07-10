package stryker4s.extensions.mutationtypes

import stryker4s.Stryker4sSuite
import stryker4s.extensions.ImplicitMutationConversion.mutationToTree
import stryker4s.scalatest.TreeEquality

import scala.meta._

class MutationTypesTest extends Stryker4sSuite with TreeEquality {
  describe("match TermNameMutation") {
    it("> to GreaterThan") {
      q">" should matchPattern { case GreaterThan(_) => }
    }

    it(">= to GreaterThanEqualTo") {
      q">=" should matchPattern { case GreaterThanEqualTo(_) => }
    }

    it("<= to LesserThanEqualTo") {
      q"<=" should matchPattern { case LesserThanEqualTo(_) => }
    }

    it("< to LesserThan") {
      q"<" should matchPattern { case LesserThan(_) => }
    }

    it("== to EqualTo") {
      q"==" should matchPattern { case EqualTo(_) => }
    }

    it("!= to NotEqualTo") {
      q"!=" should matchPattern { case NotEqualTo(_) => }
    }

    it("&& to And") {
      q"&&" should matchPattern { case And(_) => }
    }

    it("|| to Or") {
      q"||" should matchPattern { case Or(_) => }
    }

    it("filter to Filter") {
      q"filter" should matchPattern { case Filter(_) => }
    }

    it("filterNot to FilterNot") {
      q"filterNot" should matchPattern { case FilterNot(_) => }
    }

    it("should not match a different pattern") {
      q"filter" should not matchPattern { case FilterNot(_) => }
    }

    it("should return original tree on match") {
      val tree = q">="

      val result = tree match {
        case GreaterThanEqualTo(t) => t
      }

      result should be theSameInstanceAs tree
    }
  }

  describe("match LiteralMutation") {
    it("false to False") {
      q"false" should matchPattern { case False(_) => }
    }

    it("true to True") {
      q"true" should matchPattern { case True(_) => }
    }

    it("foo string to NonEmptyString") {
      q""""foo"""" should matchPattern { case NonEmptyString(_) => }
    }

    it("empty string to EmptyString") {
      Lit.String("") should matchPattern { case EmptyString(_) => }
    }
  }

  describe("implicit convert") {
    it("should convert GreaterThan to >") {
      val wrapped = WrappedTree(GreaterThan)

      wrapped.term should equal(q">")
    }

    it("should convert to the proper type") {
      val falseTree: Tree = False
      val greaterThan: Tree = GreaterThan

      falseTree shouldBe a[Lit.Boolean]
      greaterThan shouldBe a[Term.Name]
    }

    case class WrappedTree(term: Tree)
  }
}
