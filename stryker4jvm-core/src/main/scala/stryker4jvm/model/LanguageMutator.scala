package stryker4jvm.model

import stryker4jvm.core.model.{AST, CollectedMutants, Collector, Instrumenter, MutantWithId, Parser}
import stryker4jvm.core

import java.nio.file.Path
import java.util

class LanguageMutator[T <: AST](parser: Parser[T], collector: Collector[T], instrumenter: Instrumenter[T]) {
  type Tree = T

  def parse(p: Path): T = parser.parse(p)

  def collect(tree: AST): CollectedMutants[T] = {
    collector.collect(tree.asInstanceOf[T])
  }

  def instrument(source: AST, mutations: util.Map[AST, util.List[MutantWithId[AST]]]): T = {
    instrumenter.instrument(
      source.asInstanceOf[T],
      mutations.asInstanceOf[util.Map[T, util.List[core.model.MutantWithId[T]]]]
    )
  }
}
