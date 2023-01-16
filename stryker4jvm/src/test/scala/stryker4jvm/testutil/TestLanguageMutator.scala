package stryker4jvm.testutil

import stryker4jvm.core.model.elements.{Location, Position}
import stryker4jvm.core.model.{AST, CollectedMutants, Collector, Instrumenter, LanguageMutator, MutantMetaData, MutantWithId, MutatedCode, Parser}

import java.nio.file.Path
import java.util
import scala.collection.JavaConverters.*
import scala.io.Source

class MockAST(val contents: String, val children: Array[MockAST]) extends AST {
  override def syntax(): String = s"$contents, ${children.mkString("{", ", ", "}")}"

  def canEqual(other: Any): Boolean = other.isInstanceOf[MockAST]

  override def equals(other: Any): Boolean = other match {
    case that: MockAST =>
      (that canEqual this) &&
        contents == that.contents &&
        children == that.children
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(contents, children)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

class TestParser extends Parser[MockAST] {
  override def parse(path: Path): MockAST = {
    val bufferedSource = Source.fromFile(path.toFile)
    val result = new MockAST("", bufferedSource.getLines().map(new MockAST(_, Array.empty)).toArray)
    bufferedSource.close()
    result
  }
}

class TestCollector extends Collector[MockAST] {
  override def collect(t: MockAST): CollectedMutants[MockAST] = {
    val mutations =
      t.children
        .zipWithIndex
        .filter{case (ast, _) => {
        val contents = ast.contents
        contents.nonEmpty && contents.charAt(0).isUpper
      }}
      .map{case (ast, i) => {
        val oldContents = ast.contents
        val newContents = "Mutated: " + oldContents
        val mutatedCode: MutatedCode[MockAST] = new MutatedCode(new MockAST(newContents, Array.empty),
          new MutantMetaData(oldContents, newContents, "TestMutator",
            new Location(new Position(i, 0), new Position(i, 0))))
        (ast -> List(mutatedCode).asJava)
      }}.toMap
    new CollectedMutants[MockAST](List.empty.asJava, mutations.asJava)
  }
}

class TestInstrumenter extends Instrumenter[MockAST] {
  override def instrument(ast: MockAST, map: util.Map[MockAST, util.List[MutantWithId[MockAST]]]): MockAST = {
    val instrumented = ast.children.map(child => map.asScala.get(child) match {
      case Some(mutated) => mutated.asScala.head.mutatedCode.mutatedStatement
      case None => child
    })
    new MockAST(ast.contents, instrumented)
  }
}

class TestLanguageMutator(parser: TestParser = new TestParser(),
                          collector: TestCollector = new TestCollector(),
                          instrumenter: TestInstrumenter = new TestInstrumenter()
) extends LanguageMutator[MockAST](parser, collector, instrumenter)
