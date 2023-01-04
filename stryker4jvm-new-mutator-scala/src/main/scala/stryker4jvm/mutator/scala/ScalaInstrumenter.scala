package stryker4jvm.mutator.scala

import stryker4jvm.core.model.Instrumenter

import java.util as ju
import stryker4jvm.core.model.MutantWithId

import stryker4jvm.mutator.scala.extensions.TreeExtensions.TransformOnceExtension
import scala.collection.JavaConverters.*

import cats.data.{NonEmptyList, NonEmptyVector}
import cats.syntax.all.*
import stryker4jvm.mutator.scala.extensions.TreeExtensions.TransformOnceExtension
import stryker4jvm.core.logging.Logger
import stryker4jvm.core.model.MutantWithId
import stryker4jvm.core.model.InstrumenterOptions

import scala.meta.*
import scala.util.control.NonFatal
import scala.util.{Failure, Success}
import fs2.io.file.Path
import scala.util.control

class ScalaInstrumenter(instrumenterOptions: InstrumenterOptions = null) extends Instrumenter[ScalaAST] {
  override def instrument(source: ScalaAST, mutations: ju.Map[ScalaAST, ju.List[MutantWithId[ScalaAST]]]): ScalaAST = {

    val muts = mutations.asScala

    if (source.source == null) {
      return new ScalaAST // TODO
    }

    val newTree = source.source
      .transformOnce {
        Function.unlift { originalTree =>
          val p = new ScalaAST(tree = originalTree)

          val mutGet = mutations.get(p)

          if (mutGet != null) {
            val mut: Vector[MutantWithId[ScalaAST]] = mutGet.asScala.toVector

            val mutableCases = mut.map(mutantToCase)
            val default = defaultCase(p, NonEmptyList.one(0))

            val cases = mutableCases :+ default

            try Some(buildMatch(cases.toNev.get).asInstanceOf[Tree])
            catch {
              case NonFatal(e) => throw new Exception
            }
          } else {
            None
          }

        }
      } match {
      case Success(tree) => tree
      case Failure(e)    => throw e
    }

    println(s"Original:\n${source.source}\n")

    println(s"New:\n$newTree\n")

    new ScalaAST(tree = newTree)
  }

  //
  //
  //
  //
  //

  def mutantToCase(mutant: MutantWithId[ScalaAST]): Case = {
    val newTree = mutant.mutatedCode.mutatedStatement.term

    // buildCase(newTree, options.pattern(mutant.id))
    buildCase(newTree, p"0") // TODO: Do mutant IDs correctly, instead of having 0 for everything
  }

  def defaultCase(scalaAST: ScalaAST, mutantIds: NonEmptyList[Int]): Case =
    // p"case _ if ${options.condition.mapApply(mutantIds)} => ${placeableTree.tree.asInstanceOf[Term]}"
    // TODO
    p"case _ if test => ${scalaAST.tree.asInstanceOf[Term]}"

  def buildCase(expression: Term, pattern: Pat): Case = p"case $pattern => $expression"

  def buildMatch(cases: NonEmptyVector[Case]): Term.Match =
    // q"(${options.mutationContext} match { ..case ${cases.toList} })"
    // TODO
    q"(test match { ..case ${cases.toList} })"

}
