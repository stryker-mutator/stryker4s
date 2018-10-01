package stryker4s.extensions.mutationtypes

import stryker4s.Stryker4sSuite
import stryker4s.extensions.ImplicitMutationConversion.mutationToTree
import stryker4s.scalatest.TreeEquality

import scala.meta._

class MutationTypesTest extends Stryker4sSuite with TreeEquality {
  describe("BinaryOperators") {
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
  }

  describe("BooleanSubstitutions") {
    it("false to False") {
      q"false" should matchPattern { case False(_) => }
    }

    it("true to True") {
      q"true" should matchPattern { case True(_) => }
    }
  }

  describe("LogicalOperators") {
    it("&& to And") {
      q"&&" should matchPattern { case And(_) => }
    }

    it("|| to Or") {
      q"||" should matchPattern { case Or(_) => }
    }
  }

  describe("StringMutators") {
    it("foo string to NonEmptyString") {
      q""""foo"""" should matchPattern { case NonEmptyString(_) => }
    }

    it("empty string to EmptyString") {
      Lit.String("") should matchPattern { case EmptyString(_) => }
    }

    it("string interpolation to StringInterpolation") {
      Term.Interpolate(q"s", List(Lit.String("foo "), Lit.String("")), List(q"foo")) should matchPattern {
        case StringInterpolation(_) =>
      }
    }

    it("q interpolation should not match StringInterpolation") {
      Term.Interpolate(q"q", List(Lit.String("foo "), Lit.String("")), List(q"foo")) should not matchPattern {
        case StringInterpolation(_) =>
      }
    }
  }

  describe("test") {
    it("adsa") {

      val tree =  q"foo.filter(args)"
      val tree2 = q"filter(args)"
      val tree3 = q"foo.bar.filter(args)"
      val tree4 = q"foo.filter(args).bar"
      val tree5 = q"foo.filter(args).bar.filter(args2)"

      def test(tree:Tree):Unit = {
        println(tree.structure)
        println(tree.transform {
          case x @ Term.Apply(Term.Select(a, Term.Name("filter")), b :: Nil) => {
            Term.Apply(Term.Select(a, Term.Name("filterNot")), b :: Nil)
          }
          case x => x
        })
      }

      test(tree)
      test(tree2)
      test(tree3)
      test(tree4)
      test(tree5)

      true should equal(true)

    }
  }

//  describe("MethodMutators OLD") {
//    it("filter to Filter") {
//      q"filter" should matchPattern { case Filter(_) => }
//    }
//
//    it("filterNot to FilterNot") {
//      q"filterNot" should matchPattern { case FilterNot(_) => }
//    }
//
//    it("exists to Exists") {
//      q"exists" should matchPattern { case Exists(_) => }
//    }
//
//    it("forAll to ForAll") {
//      q"forAll" should matchPattern { case ForAll(_) => }
//    }
//
//    it("isEmpty to IsEmpty") {
//      q"isEmpty" should matchPattern { case IsEmpty(_) => }
//    }
//
//    it("nonEmpty to NonEmpty") {
//      q"nonEmpty" should matchPattern { case NonEmpty(_) => }
//    }
//
//    it("indexOf to IndexOf") {
//      q"indexOf" should matchPattern { case IndexOf(_) => }
//    }
//
//    it("lastIndexOf to LastIndexOf") {
//      q"lastIndexOf" should matchPattern { case LastIndexOf(_) => }
//    }
//
//    it("max to Max") {
//      q"max" should matchPattern { case Max(_) => }
//    }
//
//    it("min to Min") {
//      q"min" should matchPattern { case Min(_) => }
//    }
//
//    it("should not match a different pattern") {
//      q"filter" should not matchPattern { case FilterNot(_) => }
//    }
//  }

  describe("other cases") {
    it("should return original tree on match") {
      val tree = q">="

      val result = tree match {
        case GreaterThanEqualTo(t) => t
      }

      result should be theSameInstanceAs tree
    }

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