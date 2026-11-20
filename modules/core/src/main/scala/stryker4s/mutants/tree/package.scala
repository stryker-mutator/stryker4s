package stryker4s.mutants

import cats.data.{NonEmptyList, NonEmptyVector}
import stryker4s.model.MutantWithId

import scala.meta.Term

package object tree {

  type MutantsWithId = NonEmptyVector[MutantWithId]

  type MutantCoverageTermFn = (NonEmptyList[Int]) => Term

}
