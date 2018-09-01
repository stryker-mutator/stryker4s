package stryker4s.model

import scala.meta.{Source, Term}

case class TransformedMutants(originalStatement: Term, mutantStatements: List[Mutant])

case class SourceTransformations(source: Source, transformedStatements: Seq[TransformedMutants])
