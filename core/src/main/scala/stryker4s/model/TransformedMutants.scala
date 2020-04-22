package stryker4s.model

import scala.meta.{Source, Term}

final case class TransformedMutants(originalStatement: Term, mutantStatements: List[Mutant])

final case class SourceTransformations(source: Source, transformedStatements: Seq[TransformedMutants])
