package stryker4s.model

import scala.meta.Term

case class Mutant(id: Int, original: Term, mutated: Term)
