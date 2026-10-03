package stryker4s.mutants.tree

import fs2.io.file.Path
import stryker4s.extension.TreeExtensions.*
import stryker4s.model.*
import stryker4s.mutation.GreaterThan
import stryker4s.testkit.{LogMatchers, Stryker4sSuite}
import stryker4s.testutil.TestData

import scala.meta.*

class MutantInstrumenterScala3Test extends Stryker4sSuite with TestData with LogMatchers {

  implicit override def dialect: Dialect = dialects.Scala3

  val path = Path("foo/bar.scala")

  /** Instruments `source` with a single mutant placed on `anchor`, which has to be a node from `source` itself
    */
  def instrument(source: Source, anchor: Term, replacement: Term): MutatedFile = {
    val mutants = Map(PlaceableTree(anchor) -> toMutations(anchor, GreaterThan, replacement))
    val sut = new MutantInstrumenter(InstrumenterOptions.testRunner)

    sut.instrumentFile(SourceContext(source, path), mutants)
  }

  def endMarkersAfterInstrumenting(source: Source, anchor: Term, replacement: Term): List[String] =
    instrument(source, anchor, replacement).mutatedSource.collect { case marker: Term.EndMarker =>
      marker.name.value
    }

  def emittedTextAfterInstrumenting(source: Source, anchor: Term, replacement: Term): String =
    instrument(source, anchor, replacement).mutatedSourceText[fs2.Pure].compile.string

  def endMarkersIn(renderedFile: String): List[String] = renderedFile.parseSource.collect {
    case marker: Term.EndMarker => marker.name.value
  }

  test("instrumentFile should not leave behind an end marker whose construct was displaced") {
    val source = """object Foo:
      def foo(xs: List[String]): List[String] =
        for
          x <- xs.filter(_ == "a")
        yield x

        end for
    """.parseSource
    // A mutation in an enumerator is not placeable, so the whole for-comprehension becomes the anchor and is
    // displaced into a case body, leaving `end for` behind as a sibling of the mutation switch
    val originalStatement = source.dfsCollectFirst { case t: Term.ForYield => t }.value

    val result = endMarkersAfterInstrumenting(
      source,
      originalStatement,
      """for (x <- xs.filter(_ != "a")) yield x""".parseTerm
    )

    assertEquals(result, Nil)
  }

  test("instrumentFile should not leave behind a displaced end marker in a template body") {
    // The displaced statement is a direct child of the class body, so its marker is a `Template.Body` stat
    // instead of a `Term.Block` stat
    val source = """class Foo:
      val z = 1
      if z > 0 then println(z) else println(0)
      end if
    """.parseSource
    val originalStatement = source.dfsCollectFirst { case t: Term.If => t }.value

    val result =
      endMarkersAfterInstrumenting(source, originalStatement, "if z >= 0 then println(z) else println(0)".parseTerm)

    assertEquals(result, Nil)
  }

  test("instrumentFile should not leave behind a displaced end marker in a secondary constructor") {
    // A secondary constructor body is a `Ctor.Block`, not a `Term.Block`. `canPlace` does not currently anchor on a
    // direct statement of one, so this holder is only reachable by placing a mutant by hand as this test does
    val source = """class Foo(x: Int):
      def this(y: Int, z: Int) =
        this(y)
        if y > 0 then println(y) else println(z)
        end if
    """.parseSource
    val originalStatement = source.dfsCollectFirst { case t: Term.If => t }.value

    val result =
      endMarkersAfterInstrumenting(source, originalStatement, "if y >= 0 then println(y) else println(z)".parseTerm)

    assertEquals(result, Nil)
  }

  test("instrumentFile should keep an end marker that still follows the construct it closes") {
    // The mutation lands inside the `if` body, so the `if` itself stays in place and both markers remain bound.
    // `end if` sits in a statement list that does contain a switch, just not directly in front of the marker
    val source = """object Foo:
      def foo(x: Int): Int =
        val y =
          if x > 15 then
            1
          else
            2
          end if
        y
      end foo
    """.parseSource
    val originalStatement = source.find("1".parseTerm).value

    val result = endMarkersAfterInstrumenting(source, originalStatement, "3".parseTerm)

    assertEquals(result, List("if", "foo"))
  }

  test("instrumentFile should keep end markers of constructs that were not displaced") {
    // Every `end` specifier that closes something `canPlace` never anchors on: a `var`, an `object`, both flavours of
    // `given`, an `extension`, a pattern-binding `val`, a `def` and a `class`
    val source = """class Service:
      var count = 0
      end count

      object Helper:
        def h = 1
      end Helper

      given gi: Int = 1
      end gi

      given Conversion[Int, String] = _.toString
      end given

      extension (i: Int) def dbl = i * 2
      end extension

      def run(): Int =
        val (a, b) = (1, 2)
        end val
        count += a + b
        Helper.h
      end run
    end Service
    """.parseSource
    val originalStatement = source.find("1".parseTerm).value

    val result = endMarkersAfterInstrumenting(source, originalStatement, "3".parseTerm)

    assertEquals(result, List("count", "Helper", "gi", "given", "extension", "val", "run", "Service"))
  }

  test("instrumentFile should keep end markers of constructs that are not a mutation switch") {
    // A marker is only dropped when a switch was placed directly in front of it, so no marker in a construct
    // stryker4s did not touch is at risk, however unusual that construct is: `try e catch handler` is a
    // `Term.TryWithHandler`, `e.match` a `Term.SelectMatch`, and `case A, B` a `Defn.RepeatedEnumCase`
    val source = """class Service:
      def compute(x: Int): Int =
        try x catch handler
        end try

      def choose(x: Int): Int =
        x.match
          case _ => 0
        end match

      enum Mode:
        case A, B
        end val
    end Service
    """.parseSource
    // Anchored inside a `case` body, so the body is placeable and no construct is displaced
    val originalStatement = source.find("0".parseTerm).value

    val result = endMarkersAfterInstrumenting(source, originalStatement, "1".parseTerm)

    assertEquals(result, List("try", "match", "val", "Service"))
  }

  test("instrumentFile should keep an end marker that still binds to the switch that displaced its construct") {
    // The mutation is in the scrutinee and so is not placeable, displacing the whole `match`
    val source = """object Foo:
      def foo(x: Int): Int =
        x > 1 match
          case true  => 1
          case false => 2
        end match
    """.parseSource
    val originalStatement = source.dfsCollectFirst { case t: Term.Match => t }.value

    val result = endMarkersAfterInstrumenting(
      source,
      originalStatement,
      "x >= 1 match { case true => 1; case false => 2 }".parseTerm
    )

    assertEquals(result, List("match"))
  }

  test("instrumentFile should not emit a displaced end marker in the spliced file") {
    // The tree is not what lands on disk: the file is spliced from the original text, replacing only the range of the
    // mutated statement. The marker sits outside that range, so the splice has to widen over it
    val source = """object Foo:
      def foo(xs: List[String]): List[String] =
        for
          x <- xs.filter(_ == "a")
        yield x

        end for
    """.parseSource
    val originalStatement = source.dfsCollectFirst { case t: Term.ForYield => t }.value

    val result = emittedTextAfterInstrumenting(
      source,
      originalStatement,
      """for (x <- xs.filter(_ != "a")) yield x""".parseTerm
    )

    assertEquals(endMarkersIn(result), Nil, result)
  }

  test("instrumentFile should not emit a displaced end marker of a template body statement") {
    val source = """class Foo:
      val z = 1
      if z > 0 then println(z) else println(0)
      end if
    """.parseSource
    val originalStatement = source.dfsCollectFirst { case t: Term.If => t }.value

    val result =
      emittedTextAfterInstrumenting(source, originalStatement, "if z >= 0 then println(z) else println(0)".parseTerm)

    assertEquals(endMarkersIn(result), Nil, result)
  }

  test("instrumentFile should not emit an end marker displaced inside a nested mutation switch") {
    // Only the outermost statement is spliced, so the inner switch reaches the file through the outer switch's
    // reprint. The marker has to be gone from that tree, not just from the tree of the file as a whole
    val source = """object Foo:
      def foo(xs: List[Int]): List[Int] =
        xs.map { y =>
          if y > 0 then 1 else 2
          end if
        }
    """.parseSource
    val outer = source.dfsCollectFirst { case t: Term.Apply => t }.value
    val inner = source.dfsCollectFirst { case t: Term.If => t }.value
    val mutants = Map(
      PlaceableTree(outer) -> toMutations(outer, GreaterThan, "xs.map(y => 0)".parseTerm),
      PlaceableTree(inner) -> toMutations(inner, GreaterThan, "if y >= 0 then 1 else 2".parseTerm)
    )
    val sut = new MutantInstrumenter(InstrumenterOptions.testRunner)

    val result = sut.instrumentFile(SourceContext(source, path), mutants)

    assertEquals(result.splice.value.replacements.length, 1)
    val emitted = result.mutatedSourceText[fs2.Pure].compile.string
    assertEquals(endMarkersIn(emitted), Nil, emitted)
  }

  test("instrumentFile should emit end markers that were not displaced") {
    // `end match` still binds, and `end foo` closes a def that was never touched, so the file must keep both
    val source = """object Foo:
      def foo(x: Int): Int =
        x > 1 match
          case true  => 1
          case false => 2
        end match
      end foo
    """.parseSource
    val originalStatement = source.dfsCollectFirst { case t: Term.Match => t }.value

    val result = emittedTextAfterInstrumenting(
      source,
      originalStatement,
      "x >= 1 match { case true => 1; case false => 2 }".parseTerm
    )

    assertEquals(endMarkersIn(result), List("match", "foo"), result)
  }

  test("instrumentFile should drop a displaced end marker of any kind that can close a term") {
    // Removal is not per-kind: every marker other than `end match` is dropped when a switch lands in front of it
    val source = """object Foo:
      def whileLoop(x: Int): Unit =
        while x > 0 do println(x)
        end while

      def tryCatch(x: Int): Int =
        try
          if x > 0 then 1 else 2
        catch
          case _: Throwable => 0
        end try

      def anonClass(x: Int): AnyRef =
        new Object:
          val y = x > 0
        end new
    """.parseSource
    val whileLoop = source.dfsCollectFirst { case t: Term.While => t }.value
    val tryCatch = source.dfsCollectFirst { case t: Term.Try => t }.value
    val anonClass = source.dfsCollectFirst { case t: Term.NewAnonymous => t }.value
    val mutants = Map(
      PlaceableTree(whileLoop) -> toMutations(whileLoop, GreaterThan, "while x >= 0 do println(x)".parseTerm),
      PlaceableTree(tryCatch) -> toMutations(
        tryCatch,
        GreaterThan,
        "try (if x >= 0 then 1 else 2) catch { case _: Throwable => 0 }".parseTerm
      ),
      PlaceableTree(anonClass) -> toMutations(anonClass, GreaterThan, "new Object { val y = x >= 0 }".parseTerm)
    )
    val sut = new MutantInstrumenter(InstrumenterOptions.testRunner)

    val result = sut.instrumentFile(SourceContext(source, path), mutants)

    assertEquals(result.mutatedSource.collect { case marker: Term.EndMarker => marker.name.value }, Nil)
    assertEquals(endMarkersIn(result.mutatedSourceText[fs2.Pure].compile.string), Nil)
  }
}
