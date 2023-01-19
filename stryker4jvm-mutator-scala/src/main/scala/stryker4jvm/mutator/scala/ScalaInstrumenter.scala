package stryker4jvm.mutator.scala

import stryker4jvm.core.model.Instrumenter

import java.util as ju
import scala.collection.JavaConverters.*

import cats.data.{NonEmptyList, NonEmptyVector}
import cats.syntax.all.*
import stryker4jvm.mutator.scala.extensions.TreeExtensions.TransformOnceExtension
import stryker4jvm.core.model.MutantWithId

import scala.meta.*
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class ScalaInstrumenter(options: ScalaInstrumenterOptions) extends Instrumenter[ScalaAST] {

  /** Method that places mutations in a given AST
    *
    * @param source
    *   The input AST
    * @param mutations
    *   All the mutations found in the input AST, which are used in the returned AST
    * @return
    *   AST with all mutations placed (using match statements)
    */
  override def instrument(source: ScalaAST, mutations: ju.Map[ScalaAST, ju.List[MutantWithId[ScalaAST]]]): ScalaAST = {
    if (source.value == null) {
      return new ScalaAST // TODO
    }

    val newTree = source.value
      .transformOnce {
        Function.unlift { originalTree =>
          val p = new ScalaAST(value = originalTree)

          // Get all mutations for a given tree
          val mutGet = mutations.get(p)

          if (mutGet != null) {
            val mut: Vector[MutantWithId[ScalaAST]] = mutGet.asScala.toVector

            val mutableCases = mut.map(mutantToCase)
            val maybeNonemptyList = NonEmptyList.fromList(mut.map(_.id).toList);

            var nonEmptylist: NonEmptyList[Int] = NonEmptyList.one(0);
            maybeNonemptyList match {
              case Some(value) => nonEmptylist = value
              case None        =>
            };

            val default = defaultCase(p, nonEmptylist)
            val cases = mutableCases :+ default

            // Build the match statement and cast to a Tree
            try Some(buildMatch(cases.toNev.get).asInstanceOf[Tree])
            catch {
              case NonFatal(_) => throw new Exception
            }
          } else {
            None
          }
        }
      } match {
      case Success(tree) => tree
      case Failure(e)    => throw e
    }

    new ScalaAST(value = newTree)
  }

  def mutantToCase(mutant: MutantWithId[ScalaAST]): Case = {
    val newTree = mutant.mutatedCode.mutatedStatement.value.asInstanceOf[Term]

    buildCase(newTree, options.pattern(mutant.id))
  }

  def defaultCase(scalaAST: ScalaAST, mutantIds: NonEmptyList[Int]): Case =
    p"case _ if ${options.condition.mapApply(mutantIds)} => ${scalaAST.value.asInstanceOf[Term]}"

  def buildCase(expression: Term, pattern: Pat): Case = p"case $pattern => $expression"

  def buildMatch(cases: NonEmptyVector[Case]): Term.Match =
    q"(${options.mutationContext} match { ..case ${cases.toList} })"

}
